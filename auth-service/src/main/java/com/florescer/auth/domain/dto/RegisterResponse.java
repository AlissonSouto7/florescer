package com.florescer.auth.domain.dto;

import java.util.UUID;

public record RegisterResponse(String token, UUID userId) {}