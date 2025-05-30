package com.florescer.auth.infra.security;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class JwtServiceImpl implements JwtService {
	
	private final JwtEncoder encoder;
	private final JwtDecoder decoder;
	
    @Value("${jwt.access-expiration}")
    private Long accessExpiry;

	public String generateToken(Authentication authentication) {
		
		Instant now = Instant.now();
		
		String scopes = authentication
		        .getAuthorities().stream()
		        .map(GrantedAuthority::getAuthority)
		        .collect(Collectors
		            .joining(" "));
		
		var claims = JwtClaimsSet.builder()
				.issuer("auth-service")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(accessExpiry))
				.subject(authentication.getName())
				.claim("scope", scopes)
				.build();
		
		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
	
	@Override
	public boolean isTokenValid(String token, UserDetails userDetails) {
	    try {
	        Jwt jwt = decoder.decode(token);
	        
	        Instant now = Instant.now();
	
	        return Optional.ofNullable(jwt.getExpiresAt())
                    .map(expiration -> expiration.isAfter(now))
                    .orElse(false) && jwt.getSubject().equals(userDetails.getUsername());
	        
	    } catch (JwtException e) {
	    	log.error("Erro ao validar o token: {}", e.getMessage());
	        return false; 
	    }
	}

	@Override
	public String extractUsername(String token) {
		return decoder.decode(token).getSubject();
	}
}
