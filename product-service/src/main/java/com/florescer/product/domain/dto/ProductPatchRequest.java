package com.florescer.product.domain.dto;

import com.florescer.product.domain.enums.Status;

public record ProductPatchRequest(String name, String type, String description, Double price, Integer quantityStock,
		String careRequirements, Boolean availability, Status status) {}