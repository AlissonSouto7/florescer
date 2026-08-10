package com.florescer.auth.infrastructure.security;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Emite os tokens de acesso.
 *
 * <p>Não valida token recebido: isso é responsabilidade do resource server, pelo
 * {@code JwtDecoder}. O {@code JwtDecoder} deixou de ser injetado aqui junto com
 * a remoção dos métodos de validação, que só o filtro removido usava.
 */
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

	private final JwtEncoder encoder;

	@Value("${jwt.access-expiration}")
	private Long accessExpiry;

	@Override
	public String generateToken(Authentication authentication) {
		Instant now = Instant.now();

		String scopes = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(" "));

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("auth-service")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(accessExpiry))
				.subject(authentication.getName())
				.claim("scope", scopes)
				.build();

		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
