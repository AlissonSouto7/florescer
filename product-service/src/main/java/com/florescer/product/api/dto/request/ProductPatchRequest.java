package com.florescer.product.api.dto.request;

import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.domain.enums.Status;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductPatchRequest(
        @Schema(description = "Nome do produto", example = SwaggerConstants.NAME_EXAMPLE)
        String name,

        @Schema(description = "Tipo do produto", example = SwaggerConstants.TYPE_EXAMPLE)
        String type,

        @Schema(description = "Descrição do produto", example = SwaggerConstants.DESCRIPTION_EXAMPLE)
        String description,

        @Schema(description = "Preço do produto", example = SwaggerConstants.PRICE_EXAMPLE)
        Double price,

        @Schema(description = "Quantidade em estoque", example = SwaggerConstants.QUANTITY_EXAMPLE)
        Integer quantityStock,

        @Schema(description = "Requisitos de cuidados", example = SwaggerConstants.CARE_EXAMPLE)
        String careRequirements,

        @Schema(description = "Disponibilidade", example = SwaggerConstants.AVAILABILITY_EXAMPLE)
        Boolean availability,

        @Schema(description = "Status do produto", example = SwaggerConstants.STATUS_EXAMPLE)
        Status status) {}