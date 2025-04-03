package com.florescer.product.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.florescer.product.domain.dto.ProductCreateRequest;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductGetResponse;
import com.florescer.product.domain.dto.ProductPatchRequest;

@RequestMapping("/v1/product")
public interface ProductController {
	
	@PostMapping("/create")
	public ResponseEntity<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest request);
	
	@GetMapping("/")
	public ResponseEntity<List<ProductGetAllResponse>> getAllProduct();
	
	@GetMapping("/{productId}")
	public ResponseEntity<ProductGetResponse> getProductById(@PathVariable UUID productId);
	
	@PatchMapping("/patch/{productId}")
	public ResponseEntity<Void> patchProduct(@PathVariable UUID productId, @RequestBody ProductPatchRequest request);
	
	@DeleteMapping("/delete/{productId}")
	public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId);
}