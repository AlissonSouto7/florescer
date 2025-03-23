package com.florescer.product.domain.entity;

import java.util.UUID;

import com.florescer.product.domain.enums.Status;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Product {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID productId;
	
	private String name;
	
	private String type;
	
	private String description;
	
	private Double price;
	
	private Integer quantityStock;
	
	private String careRequirements;
	
	private Boolean availability;
	
	@Enumerated(EnumType.STRING)
	private Status status;
	
}