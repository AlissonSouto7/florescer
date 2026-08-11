package com.florescer.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.florescer.product.domain.enums.Status;
import com.florescer.product.domain.exception.custom.InvalidPatchException;
import com.florescer.product.domain.exception.custom.ProductNotFoundException;
import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.model.ProductChanges;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.domain.service.ProductService;
import com.florescer.product.support.AbstractIntegrationTest;

/**
 * O que o PATCH faz com o que não foi enviado.
 *
 * <p>A promessa de um PATCH é mexer só no que veio. Um campo ausente e um campo
 * enviado como nulo são coisas diferentes, e confundir os dois apaga dado que
 * ninguém pediu para apagar: quem edita só o preço perderia a descrição.
 *
 * <p>Estes casos entram pelo serviço, e não pelo HTTP, porque o alvo é a regra
 * de aplicação parcial em si. A camada HTTP já está coberta pela matriz de
 * autorização e pelos testes de validação.
 */
class PartialPatchTest extends AbstractIntegrationTest {

    @Autowired
    private ProductService service;

    @Autowired
    private ProductRepository repository;

    private Product original;

    @BeforeEach
    void criarProduto() {
        original = repository.save(Product.builder()
                .name("Samambaia")
                .type("Planta")
                .description("Verde e viçosa")
                .price(new BigDecimal("49.90"))
                .quantityStock(3)
                .careRequirements("Meia sombra")
                .availability(true)
                .status(Status.ATIVO)
                .imagePath("imagens/original.png")
                .build());
    }

    @Test
    @DisplayName("patch de um campo so nao apaga os outros")
    void patchDeUmCampoPreservaOResto() {
        service.patchProduct(original.getId(), somentePreco(new BigDecimal("59.90")), null);

        Product depois = repository.findById(original.getId()).orElseThrow();

        assertThat(depois.getPrice())
                .as("o campo enviado precisa mudar")
                .isEqualByComparingTo("59.90");

        assertThat(depois.getName()).as("nome não foi enviado").isEqualTo("Samambaia");
        assertThat(depois.getType()).as("tipo não foi enviado").isEqualTo("Planta");
        assertThat(depois.getDescription()).as("descrição não foi enviada").isEqualTo("Verde e viçosa");
        assertThat(depois.getQuantityStock()).as("estoque não foi enviado").isEqualTo(3);
        assertThat(depois.getCareRequirements()).as("cuidados não foram enviados").isEqualTo("Meia sombra");
        assertThat(depois.getAvailability()).as("disponibilidade não foi enviada").isTrue();
        assertThat(depois.getStatus()).as("status não foi enviado").isEqualTo(Status.ATIVO);
        assertThat(depois.getImagePath()).as("imagem não foi enviada").isEqualTo("imagens/original.png");
    }

    @Test
    @DisplayName("patch sem nenhum campo e sem imagem e recusado")
    void patchVazioERecusado() {
        // Aceitar em silêncio faria a API responder sucesso sem ter feito nada,
        // e quem chamou não teria como saber que o pedido se perdeu.
        assertThatThrownBy(() -> service.patchProduct(original.getId(), vazio(), null))
                .isInstanceOf(InvalidPatchException.class);
    }

    @Test
    @DisplayName("patch de campo em produto inexistente responde nao encontrado")
    void patchEmProdutoInexistente() {
        assertThatThrownBy(() -> service.patchProduct(
                UUID.randomUUID(), somentePreco(new BigDecimal("10.00")), null))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("patch trocando status mantem o preco intacto")
    void patchDeStatusNaoMexeNoPreco() {
        service.patchProduct(original.getId(),
                new ProductChanges(null, null, null, null, null, null, null, Status.INATIVO), null);

        Product depois = repository.findById(original.getId()).orElseThrow();

        assertThat(depois.getStatus()).isEqualTo(Status.INATIVO);
        assertThat(depois.getPrice())
                .as("trocar status não pode zerar nem alterar o preço")
                .isEqualByComparingTo("49.90");
    }

    private ProductChanges somentePreco(BigDecimal preco) {
        return new ProductChanges(null, null, null, preco, null, null, null, null);
    }

    private ProductChanges vazio() {
        return new ProductChanges(null, null, null, null, null, null, null, null);
    }
}
