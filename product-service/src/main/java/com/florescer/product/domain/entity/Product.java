package com.florescer.product.domain.entity;

import java.util.Optional;
import java.util.UUID;

import com.florescer.product.domain.dto.ProductPatchRequest;
import com.florescer.product.domain.dto.ProductCreateRequest;
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

	public Product(ProductCreateRequest request) {
		this.name = request.name();
		this.type = request.type();
		this.description = request.description();
		this.price = request.price();
		this.quantityStock = request.quantityStock();
		this.careRequirements = request.careRequirements();
		this.availability = request.availability();
		this.status = request.status();
	}

	public void update(ProductPatchRequest request) {
	    Optional.ofNullable(request.name()).ifPresent(value -> this.name = value);
	    Optional.ofNullable(request.type()).ifPresent(value -> this.type = value);
	    Optional.ofNullable(request.description()).ifPresent(value -> this.description = value);
	    Optional.ofNullable(request.price()).ifPresent(value -> this.price = value);
	    Optional.ofNullable(request.quantityStock()).ifPresent(value -> this.quantityStock = value);
	    Optional.ofNullable(request.careRequirements()).ifPresent(value -> this.careRequirements = value);
	    Optional.ofNullable(request.availability()).ifPresent(value -> this.availability = value);
	    Optional.ofNullable(request.status()).ifPresent(value -> this.status = value);
	}
}