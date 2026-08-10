package com.florescer.product.domain.exception.custom;

public class InvalidPatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidPatchException(String message) {
        super(message);
    }
}