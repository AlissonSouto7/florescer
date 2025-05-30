package com.florescer.product.api.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductCreateResponse(
        @Schema(description = "ID do produto criado")
        UUID id) {}