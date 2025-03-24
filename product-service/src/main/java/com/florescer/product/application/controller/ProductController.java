package com.florescer.product.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.florescer.product.domain.dto.ProductRequest;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductDtoResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductPutRequest;

@RequestMapping("/v1/product")
public interface ProductController {
	
	@PostMapping("/create")
	public ResponseEntity<ProductCreateResponse> createProduct(@RequestBody ProductRequest request);
	
	@GetMapping("/")
	public ResponseEntity<List<ProductGetAllResponse>> getAllProduct();
	
	@GetMapping("/{productId}")
	public ResponseEntity<ProductDtoResponse> getProductById(@PathVariable UUID productId);
	
	@PutMapping("/put/{productId}")
	public ResponseEntity<Void> putProduct(@PathVariable UUID productId, @RequestBody ProductPutRequest request);
	
	@DeleteMapping("/delete/{productId}")
	public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId);
}