package com.florescer.product.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O que a vendedora envia ao salvar os dados da loja.
 *
 * <p>Todo campo é opcional, e vazio significa "não tenho isso". Loja
 * recém-instalada não tem Instagram nem horário definido, e obrigar um valor
 * levaria alguém a inventar um: informação inventada no rodapé tem a mesma cara
 * de informação verdadeira.
 *
 * <p>As mensagens são escritas para quem vende, e não para quem programa: elas
 * aparecem ao lado do campo na tela dela.
 */
public record ShopSettingsRequest(

        /**
         * O número que recebe os pedidos, só com dígitos.
         *
         * <p>A tela envia o número já limpo, mas a regra vale aqui também: quem
         * chamar a API direto não passa pela tela. Um número mal formado vira um
         * link para conversa inexistente, e o defeito só aparece quando um
         * cliente tenta comprar, o que é tarde demais.
         *
         * <p>De 10 a 15 dígitos: 15 é o teto do padrão internacional E.164, e 10
         * cobre um fixo com DDD.
         */
        // Aceita como a pessoa digita: com espaço, parênteses, hífen, ponto e o
        // sinal de mais. A validação que importa (10 a 15 dígitos) acontece no
        // serviço, DEPOIS de tirar a pontuação: exigir aqui recusaria
        // "+55 (73) 99814-9668", que é justamente como se escreve um telefone.
        @Pattern(regexp = "^[-0-9 ()+.]*$",
                message = "Use apenas números e os sinais + ( ) - . no telefone")
        @Size(max = 30, message = "O telefone não pode passar de 30 caracteres")
        @Schema(description = "WhatsApp que recebe os pedidos, apenas dígitos", example = "5573998149668")
        String whatsappNumber,

        @Size(max = 120, message = "A cidade não pode passar de 120 caracteres")
        @Schema(description = "Cidade onde a vendedora entrega", example = "Itabuna, BA")
        String deliveryCity,

        /**
         * O perfil, aceito em qualquer formato que alguém digite.
         *
         * <p>Na prática as pessoas escrevem `@loja`, `loja`,
         * `instagram.com/loja` ou `https://www.instagram.com/loja/`. A limpeza
         * acontece no serviço, e o que chega aqui pode ser qualquer um desses;
         * o tamanho é o único limite nesta camada.
         */
        @Size(max = 120, message = "O Instagram não pode passar de 120 caracteres")
        @Schema(description = "Perfil do Instagram, com ou sem @", example = "@florescer.plantas")
        String instagramHandle,

        @Size(max = 180, message = "O horário não pode passar de 180 caracteres")
        @Schema(description = "Quando ela atende", example = "Segunda a sábado, das 8h às 18h")
        String openingHours) {
}
