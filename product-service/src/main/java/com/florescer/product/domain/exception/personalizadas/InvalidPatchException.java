package com.florescer.product.domain.exception.personalizadas;

public class InvalidPatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidPatchException(String message) {
        super(message);
    }
}