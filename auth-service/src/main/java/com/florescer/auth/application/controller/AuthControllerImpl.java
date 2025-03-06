package com.florescer.auth.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.florescer.auth.application.service.AuthService;
import com.florescer.auth.config.SecurityConfig;
import com.florescer.auth.domain.dto.LoginRequest;
import com.florescer.auth.domain.dto.LoginResponse;
import com.florescer.auth.domain.dto.RegisterRequest;
import com.florescer.auth.domain.dto.RegisterResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
@SecurityRequirement(name = SecurityConfig.SECURITY)
public class AuthControllerImpl implements AuthController {
	
	private final AuthService service;

	@Override
	@Operation(summary = "Registrar um novo usuário", description = "Cria uma nova conta de usuário no sistema.")
    @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
	@ApiResponse(responseCode = "409", description = "Usuário já existente")
	@ApiResponse(responseCode = "500", description = "Erro no servidor")
	public ResponseEntity<RegisterResponse> registerUser(@Valid RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
	}

	@Override
	@Operation(summary = "Autenticar um usuário", description = "Realiza o login e retorna um token JWT.")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
	@ApiResponse(responseCode = "500", description = "Erro no servidor")
	public ResponseEntity<LoginResponse> loginUser(@Valid LoginRequest request) {
		return ResponseEntity.ok(service.login(request));
	}
}