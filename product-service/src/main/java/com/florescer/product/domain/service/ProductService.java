package com.florescer.product.domain.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface ProductService {
	public ProductCreateResponse createProduct(ProductCreateRequest request, MultipartFile image);

	Page<ProductListResponse> getListProduct(Pageable pageable);

	public ProductGetResponse getProductById(UUID productId, HttpServletRequest request);

	public void patchProduct(UUID productId, ProductPatchRequest request, MultipartFile image);

	public void deleteProduct(UUID productId);
}