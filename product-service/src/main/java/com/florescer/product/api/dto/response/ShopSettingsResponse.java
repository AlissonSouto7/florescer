package com.florescer.product.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Os dados da loja, como a vitrine os recebe.
 *
 * <p>Esta resposta é <b>pública</b>: a vitrine é aberta, e o rodapé e o botão de
 * comprar precisam dela sem login. Por isso ela carrega apenas o que já ficaria
 * visível na página de qualquer jeito, e nada mais. Nem a data da última
 * alteração entra: é informação interna, e o que não é necessário não se expõe.
 *
 * <p>Campo nulo significa "a vendedora não preencheu", e quem exibe deve omitir
 * a linha inteira em vez de mostrar um rótulo sem valor.
 */
public record ShopSettingsResponse(

        @Schema(description = "WhatsApp que recebe os pedidos, apenas dígitos", example = "5511987654321")
        String whatsappNumber,

        @Schema(description = "Cidade onde a vendedora entrega", example = "São Paulo, SP")
        String deliveryCity,

        /** Sempre sem o arroba: quem monta o link decide como exibir. */
        @Schema(description = "Perfil do Instagram, sem @", example = "florescer.plantas")
        String instagramHandle,

        @Schema(description = "Quando ela atende", example = "Segunda a sábado, das 8h às 18h")
        String openingHours) {
}
