package com.florescer.auth.infrastructure.security;

import org.springframework.security.core.Authentication;

/**
 * Emissão de tokens de acesso.
 *
 * <p>A validação não está aqui: quem valida token recebido é o resource server
 * do Spring Security, através do {@code JwtDecoder}. Esta interface chegou a
 * declarar {@code isTokenValid} e {@code extractUsername}, usados apenas pelo
 * filtro removido, e mantê-los daria a impressão de que a validação passa por
 * este serviço.
 */
public interface JwtService {

	String generateToken(Authentication authentication);
}
