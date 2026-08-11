package com.florescer.auth.application.controller;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Publica a chave pública de assinatura no formato JWKS (RFC 7517).
 *
 * <p>Antes disso, um serviço que precisasse validar os tokens recebia a chave por
 * configuração própria. Trocar de chave exigia atualizar todo mundo em sincronia,
 * e qualquer defasagem entre um serviço e outro derrubava os logins. Com o JWKS,
 * quem valida busca a chave sozinho e a troca deixa de ser uma operação
 * coordenada.
 */
@RestController
@Tag(name = "JWKS", description = "Chave pública para validação dos tokens")
public class JwksController {

	private final JWKSet publicJwks;

	public JwksController(RSAKey rsaKey) {
		// toPublicJWK descarta os parâmetros privados (d, p, q, dp, dq, qi). Sem
		// isso, a chave que assina todos os tokens do sistema ficaria exposta num
		// endpoint público, que é o pior vazamento possível neste serviço.
		this.publicJwks = new JWKSet(rsaKey.toPublicJWK());
	}

	@GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Chaves públicas de assinatura",
			description = "Formato JWKS (RFC 7517). Usado por quem valida os tokens emitidos por este serviço.")
	public ResponseEntity<Map<String, Object>> jwks() {
		return ResponseEntity.ok()
				// O cache evita que cada validação vire uma requisição a este
				// endpoint. Cinco minutos é curto o bastante para uma chave nova
				// circular rápido numa rotação.
				.cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
				.body(publicJwks.toJSONObject());
	}
}
