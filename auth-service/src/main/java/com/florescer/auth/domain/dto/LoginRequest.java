package com.florescer.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "O e-mail é obrigatório") 
		@Email(message = "E-mail inválido") 
		String email,

		@NotBlank(message = "A senha é obrigatória") 
		@Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres") 
		String password
) {}