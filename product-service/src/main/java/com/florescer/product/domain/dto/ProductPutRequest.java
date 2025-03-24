package com.florescer.product.domain.dto;

import com.florescer.product.domain.enums.Status;

public record ProductPutRequest(String name, String type, String description, Double price, String careRequirements,
		Boolean availability, Status status) {}