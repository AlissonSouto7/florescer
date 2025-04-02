package com.florescer.product.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.florescer.product.application.repository.ProductRepository;
import com.florescer.product.domain.dto.ProductCreateRequest;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductGetResponse;
import com.florescer.product.domain.dto.ProductMapper;
import com.florescer.product.domain.dto.ProductPatchRequest;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.exception.ProductNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository repository;

	@Override
	public ProductCreateResponse createProduct(ProductCreateRequest request) {
		Product product = repository.save(new Product(request));
		return new ProductCreateResponse(product.getProductId());
	}

	@Override
	public List<ProductGetAllResponse> getListProduct() {
		List<Product> listProduct = repository.getListProduct();
		return ProductMapper.toGetAllResponse(listProduct);
	}

	@Override
	public ProductGetResponse getProductById(UUID productId) {
		Product product = repository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));
		return ProductMapper.toGetResponse(product);
	}

	@Override
	public void patchProduct(UUID productId, ProductPatchRequest request) {
		Product product = repository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));

		checkIfThereWasUpdate(request);

		product.update(request);
		repository.save(product);
	}

	@Override
	public void deleteProduct(UUID productId) {
		Product product = repository.findById(productId)
				.orElseThrow(() -> new ProductNotFoundException(productId));

		repository.delete(product);
	}
	
	private void checkIfThereWasUpdate(ProductPatchRequest request) {
		
		boolean isEmpty = Arrays.stream(ProductPatchRequest.class.getDeclaredMethods())
	            .filter(method -> method.getName().startsWith("get") || method.getName().startsWith("is"))
	            .allMatch(method -> {
	                try {
	                    return method.invoke(request) == null;
	                } catch (Exception e) {
	                    return false;
	                }
	            });
		if (isEmpty) {
			throw new IllegalArgumentException("Nenhum campo foi enviado para atualização.");
		}
	}
}