package com.florescer.product.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.florescer.product.application.repository.ProductRepository;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductDtoResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductPutRequest;
import com.florescer.product.domain.dto.ProductRequest;
import com.florescer.product.domain.entity.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
	
	private final ProductRepository repository;

	@Override
	public ProductCreateResponse createProduct(ProductRequest request) {
		Product product = repository.save(new Product(request));
		return new ProductCreateResponse(product.getProductId());
	}

	@Override
	public List<ProductGetAllResponse> getListProduct() {
		List<Product> listProduct = repository.getListProduct();
		return ProductGetAllResponse.convert(listProduct);
	}

	@Override
	public Optional<ProductDtoResponse> getProductById(UUID productId) {
		Optional<Product> product = repository.findById(productId);
		return product.map(ProductDtoResponse::convert);
	}
	
	@Override
	public void putProduct(UUID productId, ProductPutRequest request) {
		Optional<Product> existingProduct = repository.findById(productId);
		Product product = existingProduct.get();
		product.update(request);
		repository.save(product);
	}

	@Override
	public void deleteProduct(UUID productId) {
		Optional<Product> product = repository.findById(productId);
		repository.delete(product.get());
	}
}