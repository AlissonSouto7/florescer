package com.florescer.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "E-mail inválido")
		String email,

		// Sem restrição de formato: no login, a senha ou confere ou não confere.
		// Validar o formato aqui devolveria 400 explicando a política a quem
		// tenta adivinhar, e trancaria fora quem cadastrou antes de a política
		// mudar. Senha errada é 401, e só.
		@NotBlank(message = "A senha é obrigatória")
		String password
) {

	/**
	 * Omite a senha e o e-mail.
	 *
	 * <p>O {@code toString} gerado automaticamente por um record inclui todos os
	 * campos, e o Spring MVC registra o objeto desserializado em nível DEBUG. Com
	 * o padrão, subir o nível de log para investigar um incidente passaria a
	 * gravar a senha de cada login em texto puro.
	 */
	@Override
	public String toString() {
		return "LoginRequest[email=***, password=***]";
	}
}
