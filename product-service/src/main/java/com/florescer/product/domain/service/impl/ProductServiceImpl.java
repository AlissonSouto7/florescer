package com.florescer.product.domain.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.mapper.ProductMapper;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.exception.personalizadas.FileStorageException;
import com.florescer.product.domain.exception.personalizadas.InvalidPatchException;
import com.florescer.product.domain.exception.personalizadas.ProductNotFoundException;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.infra.storage.ImageStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ImageStorageService imageStorageService;
	private final ProductRepository repository;

	@Override
	public ProductCreateResponse createProduct(ProductCreateRequest request, MultipartFile image) {
		
        if (image == null || image.isEmpty()) {
            throw new FileStorageException("Imagem não enviada ou vazia");
        }
		
		String imagePath = imageStorageService.saveImage(image);
		
		Product product = repository.save((ProductMapper.fromCreateRequest(request, imagePath)));
		return new ProductCreateResponse(product.getId());
	}

	@Override
	public List<ProductListResponse> getListProduct() {
		List<Product> listProduct = repository.findAll();
		return ProductMapper.toGetAllResponse(listProduct);
	}

	@Override
	public ProductGetResponse getProductById(UUID id) {
		Product product = getProductOrThrow(id);
		return ProductMapper.toGetResponse(product);
	}

	@Override
	public void patchProduct(UUID id, ProductPatchRequest request) {
		Product product = getProductOrThrow(id);

        if (isRequestEmpty(request)) {
        	throw new InvalidPatchException("Nenhum campo foi enviado para atualização.");
        }

        ProductMapper.applyPatch(product, request);
		repository.save(product);
	}

	@Override
	public void deleteProduct(UUID id) {
		Product product = getProductOrThrow(id);
		repository.delete(product);
	}
	
    private Product getProductOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
	
	private boolean isRequestEmpty(ProductPatchRequest request) {
        return Stream.of(request.name(), request.type(), request.description(), request.price(),
                request.quantityStock(), request.careRequirements(), request.availability(), request.status())
                .allMatch(Objects::isNull);
    }
}