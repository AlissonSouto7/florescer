package com.florescer.product.domain.model;

/**
 * O que a vendedora quer mudar nos dados da loja.
 *
 * <p>Existe para o serviço não depender do DTO da API: o domínio fala a própria
 * língua, e mudar o formato do JSON não obriga a mexer aqui. Segue o mesmo
 * caminho que {@code ProductChanges} já abriu no catálogo.
 *
 * <p>Os valores chegam como a pessoa digitou. A limpeza (tirar o arroba do
 * Instagram, apagar tudo que não é dígito do telefone) acontece no serviço,
 * porque é regra de negócio e não formatação de tela: quem chamar a API direto
 * precisa da mesma garantia.
 */
public record ShopSettingsChanges(
        String whatsappNumber,
        String deliveryCity,
        String instagramHandle,
        String openingHours) {
}
