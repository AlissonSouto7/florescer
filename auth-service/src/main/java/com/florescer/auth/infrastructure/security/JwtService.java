package com.florescer.auth.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    
	String extractUsername(String token);
    
    boolean isTokenValid(String token, UserDetails userDetails);
}
