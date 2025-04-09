package com.florescer.product.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.request.ProductCreateRequest;
import com.florescer.product.api.dto.request.ProductPatchRequest;
import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;

@RequestMapping("/v1/product")
public interface ProductController {
	
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductCreateResponse> createProduct(@RequestPart("product") ProductCreateRequest request, 
			@RequestPart("image") MultipartFile image);
	
	@GetMapping
	public ResponseEntity<List<ProductListResponse>> listAll();
	
	@GetMapping("/{id}")
	public ResponseEntity<ProductGetResponse> findById(@PathVariable UUID id);
	
	@PatchMapping("/{id}")
	public ResponseEntity<Void> updatePartial(@PathVariable UUID id, @RequestBody ProductPatchRequest request);
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id);
}