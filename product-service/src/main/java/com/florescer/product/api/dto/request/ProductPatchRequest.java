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
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Campos de atualização parcial: cada um é opcional, e ausente significa
 * "não mexa".
 *
 * <p>Por isso não há {@code @NotBlank} nem {@code @NotNull} aqui. As restrições
 * presentes só valem quando o campo vem preenchido: se o preço for enviado, ele
 * precisa ser positivo; se não for enviado, permanece o que estava.
 */
public record ProductPatchRequest(
        @Size(max = 255, message = "Nome não pode ultrapassar 255 caracteres")
        @Schema(description = "Nome do produto", example = SwaggerConstants.NAME_EXAMPLE)
        String name,

        @Size(max = 100, message = "Tipo não pode ultrapassar 100 caracteres")
        @Schema(description = "Tipo do produto", example = SwaggerConstants.TYPE_EXAMPLE)
        String type,

        @Size(max = 500, message = "Descrição não pode ultrapassar 500 caracteres")
        @Schema(description = "Descrição do produto", example = SwaggerConstants.DESCRIPTION_EXAMPLE)
        String description,

        @Positive(message = "Preço deve ser maior que zero")
        @Schema(description = "Preço do produto", example = SwaggerConstants.PRICE_EXAMPLE)
        BigDecimal price,

        @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
        @Schema(description = "Quantidade em estoque", example = SwaggerConstants.QUANTITY_EXAMPLE)
        Integer quantityStock,

        @Size(max = 500, message = "Requisitos de cuidados não podem ultrapassar 500 caracteres")
        @Schema(description = "Requisitos de cuidados", example = SwaggerConstants.CARE_EXAMPLE)
        String careRequirements,

        @Schema(description = "Disponibilidade", example = SwaggerConstants.AVAILABILITY_EXAMPLE)
        Boolean availability,

        @Schema(description = "Status do produto", example = SwaggerConstants.STATUS_EXAMPLE)
        Status status,

        @Positive(message = "Altura deve ser maior que zero")
        @Max(value = 1000, message = "Altura não pode passar de 1000 cm")
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
        Boolean includesPot) {
}
