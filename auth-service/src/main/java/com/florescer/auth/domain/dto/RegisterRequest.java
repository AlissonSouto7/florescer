package com.florescer.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "O nome é obrigatório")
		@Size(min = 3, max = 50, message = "O nome deve ter entre 3 e 50 caracteres")
		String name,

		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "E-mail inválido")
		String email,

		// O mínimo de 12 favorece frase em vez de palavra com símbolos: é mais
		// fácil de lembrar e mais custosa de quebrar que "S3nh@!" com 6.
		//
		// O teto existe por limite do BCrypt, que ignora o que passa de 72 bytes:
		// sem ele, uma senha longa seria truncada em silêncio e o usuário
		// acreditaria ter mais proteção do que tem. Não é política, é o
		// algoritmo. Como um caractere acentuado ocupa mais de um byte, o limite
		// em caracteres fica abaixo de 72 para não estourar.
		@NotBlank(message = "A senha é obrigatória")
		@Size(min = 12, max = 64, message = "A senha deve ter entre 12 e 64 caracteres")
		String password
) {

	/**
	 * Omite a senha e o e-mail. Ver {@link LoginRequest#toString()}.
	 */
	@Override
	public String toString() {
		return "RegisterRequest[name=***, email=***, password=***]";
	}
}
