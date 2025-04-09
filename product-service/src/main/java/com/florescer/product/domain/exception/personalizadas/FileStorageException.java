package com.florescer.product.domain.exception.personalizadas;

public class FileStorageException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public FileStorageException(String message) {
		super(message);
	}
}