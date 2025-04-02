package com.florescer.product.domain.dto;

import java.util.UUID;

import com.florescer.product.domain.enums.Status;

public record ProductGetResponse(UUID productId, String name, String type, String description, Double price,
		Integer quantityStock, String careRequirements, Boolean availability, Status status) {

}