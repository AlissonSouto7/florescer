package com.florescer.product.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.florescer.product.api.dto.response.ProductCreateResponse;
import com.florescer.product.api.dto.response.ProductGetResponse;
import com.florescer.product.api.dto.response.ProductListResponse;

import jakarta.servlet.http.HttpServletRequest;

@RequestMapping("/v1/product")
public interface ProductController {
	
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ProductCreateResponse> create(@RequestPart("product") String request, 
			@RequestPart("image") MultipartFile image);
	
	@GetMapping
	public ResponseEntity<Page<ProductListResponse>> listAll(int page, int size, String[] sort);
	
	@GetMapping("/{id}")
	public ResponseEntity<ProductGetResponse> findById(@PathVariable UUID id, HttpServletRequest request);
	
	@PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> updatePartial(@PathVariable UUID id, @RequestPart("product") String request,
			@RequestPart(value = "image") MultipartFile image);
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id);
}