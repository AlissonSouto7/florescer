package com.florescer.product.api.dto.request;

import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.domain.enums.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank @Size(max = 255, message = "Nome não pode ultrapassar 255 caracteres")
        @Schema(description = "Nome do produto", example = SwaggerConstants.NAME_EXAMPLE)
        String name,

        @NotBlank @Size(max = 100, message = "Tipo não pode ultrapassar 100 caracteres")
        @Schema(description = "Tipo de produto", example = SwaggerConstants.TYPE_EXAMPLE)
        String type,

        @NotBlank @Size(max = 500, message = "Descrição não pode ultrapassar 500 caracteres")
        @Schema(description = "Descrição do produto", example = SwaggerConstants.DESCRIPTION_EXAMPLE)
        String description,

        @NotNull
        @Schema(description = "Preço do produto", example = SwaggerConstants.PRICE_EXAMPLE)
        Double price,

        @NotNull
        @Schema(description = "Quantidade em estoque", example = SwaggerConstants.QUANTITY_EXAMPLE)
        Integer quantityStock,

        @NotBlank @Size(max = 500, message = "Requisitos de cuidados não podem ultrapassar 500 caracteres")
        @Schema(description = "Cuidados necessários com o produto", example = SwaggerConstants.CARE_EXAMPLE)
        String careRequirements,

        @NotNull
        @Schema(description = "Disponibilidade do produto", example = SwaggerConstants.AVAILABILITY_EXAMPLE)
        Boolean availability,

        @NotNull
        @Schema(description = "Status do produto", example = SwaggerConstants.STATUS_EXAMPLE)
        Status status) {}