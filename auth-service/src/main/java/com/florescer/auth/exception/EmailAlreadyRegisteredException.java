package com.florescer.auth.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public EmailAlreadyRegisteredException(String email) {
        super("This Email has already been registered with: " + email);
    }
}
