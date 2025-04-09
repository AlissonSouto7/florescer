package com.florescer.product.domain.entity;
import java.util.UUID;

import com.florescer.product.domain.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@NotNull
	@Column(name = "image_path")
	private String imagePath;

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