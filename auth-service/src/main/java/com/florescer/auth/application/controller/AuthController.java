package com.florescer.auth.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.florescer.auth.domain.dto.LoginRequest;
import com.florescer.auth.domain.dto.LoginResponse;
import com.florescer.auth.domain.dto.RegisterRequest;
import com.florescer.auth.domain.dto.RegisterResponse;

import jakarta.validation.Valid;

@RequestMapping("/v1/auth")
public interface AuthController {
	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> registerUser(@RequestBody @Valid RegisterRequest request);
	
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> loginUser(@RequestBody @Valid LoginRequest request);
}