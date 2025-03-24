package com.florescer.product.domain.entity;

import java.util.UUID;

import com.florescer.product.domain.dto.ProductPutRequest;
import com.florescer.product.domain.dto.ProductRequest;
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
	
	
	public Product(ProductRequest request) {
		this.name = request.name();
		this.type = request.type();
		this.description = request.description();
		this.price = request.price();
		this.quantityStock = request.quantityStock();
		this.careRequirements = request.careRequirements();
		this.availability = request.availability();
		this.status = request.status();
	}


	public void update(ProductPutRequest request) {
		if (request.name() != null) this.name = request.name();
        if (request.type() != null) this.type = request.type();
        if (request.description() != null) this.description = request.description();
        if (request.price() != null) this.price = request.price();
        if (request.careRequirements() != null) this.careRequirements = request.careRequirements();
        if (request.availability() != null) this.availability = request.availability();
        if (request.status() != null) this.status = request.status();
	}
}