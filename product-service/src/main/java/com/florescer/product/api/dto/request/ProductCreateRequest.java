package com.florescer.product.api.dto.request;

import java.math.BigDecimal;
import com.florescer.product.api.utils.SwaggerConstants;
import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.enums.Watering;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

        @NotNull @Positive(message = "Preço deve ser maior que zero")
        @Schema(description = "Preço do produto", example = SwaggerConstants.PRICE_EXAMPLE)
        BigDecimal price,

        @NotNull @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
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
        Status status,

        @NotNull @Positive(message = "Altura deve ser maior que zero")
        @Max(value = 1000, message = "Altura não pode passar de 1000 cm")
        @Schema(description = "Altura aproximada em centímetros", example = "40")
        Integer heightCm,

        @NotNull(message = "Informe a luminosidade que a planta suporta")
        @Schema(description = "Luminosidade suportada", example = "MEIA_SOMBRA")
        Light light,

        @NotNull(message = "Informe a frequência de rega")
        @Schema(description = "Frequência de rega", example = "SEMANAL")
        Watering watering,

        @NotNull(message = "Informe se a planta é segura para animais")
        @Schema(description = "Se convive com gato ou cachorro sem risco", example = "true")
        Boolean petSafe,

        @NotNull(message = "Informe se a planta é de ambiente interno ou externo")
        @Schema(description = "Onde a planta vive bem", example = "INTERNO")
        Environment environment,

        @NotNull(message = "Informe o nível de cuidado exigido")
        @Schema(description = "Quanto cuidado a planta exige", example = "FACIL")
        Difficulty difficulty,

        @NotNull(message = "Informe se o vaso está incluso")
        @Schema(description = "Se o preço inclui o vaso", example = "true")
        Boolean includesPot) {}