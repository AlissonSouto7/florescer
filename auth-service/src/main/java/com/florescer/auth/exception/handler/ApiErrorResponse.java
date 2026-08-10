package com.florescer.auth.exception.handler;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo padrão das respostas de erro.
 *
 * @param error   o que aconteceu, em uma frase curta
 * @param details detalhe legível, ou um mapa de campo para mensagem quando o
 *                erro é de validação. É {@code Object} porque as duas formas
 *                são úteis: texto para erro único, objeto para erro por campo,
 *                que o cliente consegue associar ao formulário.
 */
@Schema(description = "Resposta padrão de erro")
public record ApiErrorResponse(
        @Schema(description = "Resumo do erro", example = "Erro de validação")
        String error,

        @Schema(description = "Detalhe do erro, texto ou mapa de campo para mensagem",
                example = "{\"email\": \"E-mail inválido\"}")
        Object details) {
}
