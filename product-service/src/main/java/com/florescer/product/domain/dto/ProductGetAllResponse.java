package com.florescer.product.domain.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Status;

public record ProductGetAllResponse(String name, String type, String description, Double price, Integer quantityStock,
		String careRequirements, Boolean availability, Status status) {
	
	public static List<ProductGetAllResponse> convert(List<Product> listProduct) {
		return listProduct.stream().map(ProductGetAllResponse::from).collect(Collectors.toList());
	}

	public static ProductGetAllResponse from(Product product) {
		return new ProductGetAllResponse(product.getName(), product.getType(), product.getDescription(),
				product.getPrice(), product.getQuantityStock(), product.getCareRequirements(),
				product.getAvailability(), product.getStatus());
	}
}