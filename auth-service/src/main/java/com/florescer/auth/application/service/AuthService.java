package com.florescer.auth.application.service;

import com.florescer.auth.domain.dto.LoginRequest;
import com.florescer.auth.domain.dto.LoginResponse;
import com.florescer.auth.domain.dto.RegisterRequest;
import com.florescer.auth.domain.dto.RegisterResponse;

import jakarta.validation.Valid;

public interface AuthService {

	RegisterResponse register(@Valid RegisterRequest request);
	
	LoginResponse login(@Valid LoginRequest request);
}