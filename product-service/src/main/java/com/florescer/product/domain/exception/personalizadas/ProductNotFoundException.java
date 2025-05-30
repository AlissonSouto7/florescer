package com.florescer.product.domain.exception.personalizadas;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	
	public ProductNotFoundException(UUID productId) {
		super("Produto não encontrado com o ID: " + productId);
	}
}