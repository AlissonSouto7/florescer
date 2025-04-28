package com.florescer.product.domain.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductListResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;

public interface ProductService {
	public ProductCreateResponse createProduct(ProductCreateRequest request, MultipartFile image);

	public List<ProductListResponse> getListProduct();

	public ProductGetResponse findById(UUID productId);

	public void patchProduct(UUID productId, ProductPatchRequest request);

	public void deleteProduct(UUID productId);
}