package com.florescer.product.domain.exception.personalizadas;

public class DatabaseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DatabaseException(String message,Throwable cause) {
		super(message, cause);
	}
}