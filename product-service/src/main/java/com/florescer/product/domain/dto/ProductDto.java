package com.florescer.product.domain.dto;

import com.florescer.product.domain.enums.Status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductDto(
		@NotBlank
		@Size(max = 255, message = "Nome não pode ultrapassar 255 caracteres")
		String name, 
		
		@NotBlank
		@Size(max = 100, message = "Tipo não pode ultrapassar 100 caracteres")
		String type, 
		
		@NotBlank
		@Size(max = 500, message = "Descrição não pode ultrapassar 500 caracteres")
		String description, 
		
		@NotNull
		Double price, 
		
		@NotNull
		Integer quantityStock,
		
		@NotBlank
		@Size(max = 500, message = "Requisitos de cuidados não podem ultrapassar 500 caracteres")
		String careRequirements,
		
		@NotNull
		Boolean availability, 
		
		@NotNull
		Status status) {}