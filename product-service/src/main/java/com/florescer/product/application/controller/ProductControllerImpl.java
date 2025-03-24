package com.florescer.product.application.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.florescer.product.application.service.ProductService;
import com.florescer.product.domain.dto.ProductCreateResponse;
import com.florescer.product.domain.dto.ProductDtoResponse;
import com.florescer.product.domain.dto.ProductGetAllResponse;
import com.florescer.product.domain.dto.ProductPutRequest;
import com.florescer.product.domain.dto.ProductRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductControllerImpl implements ProductController {
	
	private final ProductService service;

	@Override
	public ResponseEntity<ProductCreateResponse> createProduct(ProductRequest request) {
		ProductCreateResponse product = service.createProduct(request);
		return new ResponseEntity<>(product, HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<List<ProductGetAllResponse>> getAllProduct() {
		List<ProductGetAllResponse> productList = service.getListProduct();
		return new ResponseEntity<>(productList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ProductDtoResponse> getProductById(UUID productId) {
		Optional<ProductDtoResponse> product = service.getProductById(productId);
		return product.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
				.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@Override
	public ResponseEntity<Void> putProduct(UUID productId, ProductPutRequest request) {
		service.putProduct(productId, request);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@Override
	public ResponseEntity<Void> deleteProduct(UUID productId) {
		service.deleteProduct(productId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}