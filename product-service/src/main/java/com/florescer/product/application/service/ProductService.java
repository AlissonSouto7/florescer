package com.florescer.product.application.service;

import java.util.List;
import java.util.UUID;

import com.florescer.product.domain.dto.ProductCreateRequest;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductGetResponse;
import com.florescer.product.domain.dto.ProductPatchRequest;

public interface ProductService {
	
	public ProductCreateResponse createProduct(ProductCreateRequest request);

	public List<ProductGetAllResponse> getListProduct();

	public ProductGetResponse getProductById(UUID productId);

	public void patchProduct(UUID productId, ProductPatchRequest request);

	public void deleteProduct(UUID productId);

}