package com.florescer.auth.exception.custom;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class EmailNotFoundException extends UsernameNotFoundException{
	private static final long serialVersionUID = 1L;

	public EmailNotFoundException(String msg) {
		super(msg);
	}
	
}
