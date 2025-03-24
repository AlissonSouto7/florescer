package com.florescer.product.domain.dto;

import java.util.UUID;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Status;

public record ProductDtoResponse(UUID productId, String name, String type, String description, Double price,
		Integer quantityStock, String careRequirements, Boolean availability, Status status) {

	public static ProductDtoResponse convert(Product product) {
		return new ProductDtoResponse(product.getProductId(), product.getName(), product.getType(),
				product.getDescription(), product.getPrice(), product.getQuantityStock(), product.getCareRequirements(),
				product.getAvailability(), product.getStatus());
	}
}