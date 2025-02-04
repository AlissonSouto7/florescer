package com.florescer.auth.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.florescer.auth.application.service.AuthService;
import com.florescer.auth.domain.dto.LoginRequest;
import com.florescer.auth.domain.dto.LoginResponse;
import com.florescer.auth.domain.dto.RegisterRequest;
import com.florescer.auth.domain.dto.RegisterResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {
	
	private final AuthService service;

	@Override
	public ResponseEntity<RegisterResponse> registerUser(@Valid RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
	}

	@Override
	public ResponseEntity<LoginResponse> loginUser(@Valid LoginRequest request) {
		return ResponseEntity.ok(service.login(request));
	}
}