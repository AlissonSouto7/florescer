package com.florescer.product.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductDtoResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductPutRequest;
import com.florescer.product.domain.dto.ProductRequest;

public interface ProductService {
	
	public ProductCreateResponse createProduct(ProductRequest request);

	public List<ProductGetAllResponse> getListProduct();

	public Optional<ProductDtoResponse> getProductById(UUID productId);

	public void putProduct(UUID productId, ProductPutRequest request);

	public void deleteProduct(UUID productId);

}