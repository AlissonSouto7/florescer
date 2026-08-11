package com.florescer.product.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.enums.Watering;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductGetResponse(
        @Schema(description = "ID do produto")
        UUID id,

        @Schema(description = "Nome do produto", example = SwaggerConstants.NAME_EXAMPLE)
        String name,

        @Schema(description = "Tipo do produto", example = SwaggerConstants.TYPE_EXAMPLE)
        String type,

        @Schema(description = "Descrição do produto", example = SwaggerConstants.DESCRIPTION_EXAMPLE)
        String description,

        @Schema(description = "Preço do produto", example = SwaggerConstants.PRICE_EXAMPLE)
        BigDecimal price,

        @Schema(description = "Quantidade em estoque", example = SwaggerConstants.QUANTITY_EXAMPLE)
        Integer quantityStock,

        @Schema(description = "Cuidados necessários", example = SwaggerConstants.CARE_EXAMPLE)
        String careRequirements,

        @Schema(description = "Disponibilidade", example = SwaggerConstants.AVAILABILITY_EXAMPLE)
        Boolean availability,

        @Schema(description = "Status do produto", example = SwaggerConstants.STATUS_EXAMPLE)
        Status status,
        @Schema(description = "imagem do produto")
        String imageUrl,
        @Schema(description = "Altura aproximada em centímetros", example = "40")
        Integer heightCm,
        @Schema(description = "Luminosidade suportada", example = "MEIA_SOMBRA")
        Light light,
        @Schema(description = "Frequência de rega", example = "SEMANAL")
        Watering watering,
        @Schema(description = "Se convive com gato ou cachorro sem risco", example = "true")
        Boolean petSafe,
        @Schema(description = "Onde a planta vive bem", example = "INTERNO")
        Environment environment,
        @Schema(description = "Quanto cuidado a planta exige", example = "FACIL")
        Difficulty difficulty,
        @Schema(description = "Se o preço inclui o vaso", example = "true")
        Boolean includesPot) {}