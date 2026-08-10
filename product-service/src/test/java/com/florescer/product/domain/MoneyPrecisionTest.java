package com.florescer.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.enums.Status;
import com.florescer.product.infra.repository.ProductRepository;
import com.florescer.product.support.AbstractIntegrationTest;

/**
 * Dinheiro precisa voltar do banco exatamente como entrou.
 *
 * <p>Com {@code Double}, o valor era gravado como ponto flutuante binário, que
 * não representa a maioria dos decimais de forma exata. O erro é pequeno por
 * item e cresce ao somar, virando diferença de centavos que ninguém consegue
 * explicar num fechamento.
 */
class MoneyPrecisionTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository repository;

    @Test
    @DisplayName("o preco volta do banco com o mesmo valor e a mesma escala")
    void precoVoltaIntacto() {
        BigDecimal preco = new BigDecimal("19.99");

        Product salvo = repository.save(produto("Preço exato", preco));
        Product lido = repository.findById(salvo.getId()).orElseThrow();

        // compareTo em vez de equals: equals de BigDecimal considera a escala,
        // e o que importa aqui é o valor.
        assertThat(lido.getPrice())
                .as("o valor gravado precisa ser idêntico ao informado")
                .isEqualByComparingTo(preco);
        assertThat(lido.getPrice().scale())
                .as("a escala é fixada em 2 pela coluna, então centavos não somem")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("somar precos nao acumula erro")
    void somarPrecosNaoAcumulaErro() {
        // 0.1 + 0.2 em ponto flutuante dá 0.30000000000000004. Com BigDecimal
        // a conta fecha, que é o que permite confiar num total de carrinho.
        List<BigDecimal> precos = List.of(new BigDecimal("0.10"), new BigDecimal("0.20"));

        BigDecimal total = precos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(total).isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("as datas de auditoria sao preenchidas sozinhas")
    void datasDeAuditoriaSaoPreenchidas() {
        Instant antes = Instant.now().minusSeconds(1);

        Product salvo = repository.save(produto("Com auditoria", new BigDecimal("10.00")));

        assertThat(salvo.getCreatedAt())
                .as("sem isso não há como responder quando o produto entrou no catálogo")
                .isNotNull()
                .isAfter(antes);
        assertThat(salvo.getUpdatedAt()).isNotNull();
    }

    private Product produto(String nome, BigDecimal preco) {
        return Product.builder()
                .name(nome)
                .type("Flor")
                .description("Descrição")
                .price(preco)
                .quantityStock(5)
                .careRequirements("Regar")
                .availability(true)
                .status(Status.ATIVO)
                .imagePath("imagem.png")
                .build();
    }
}
