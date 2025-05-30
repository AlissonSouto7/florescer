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

		@NotBlank(message = "A senha é obrigatória") 
		@Size(min = 6, max = 20, message = "A senha deve ter entre 6 e 20 caracteres") 
		String password
) {}