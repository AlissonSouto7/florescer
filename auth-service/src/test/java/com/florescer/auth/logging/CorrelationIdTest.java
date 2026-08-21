package com.florescer.auth.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.infrastructure.logging.CorrelationIdFilter;
import com.florescer.auth.support.AbstractIntegrationTest;
import com.florescer.auth.support.LogCapture;

/**
 * O identificador que liga as linhas de log de uma mesma requisição.
 *
 * <p>Sem ele, uma chamada que passa por este serviço e depois pelo de produtos
 * deixa rastro nos dois lados sem nada em comum, e investigar um erro relatado
 * por alguém vira busca por horário aproximado.
 */
@AutoConfigureMockMvc
class CorrelationIdTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("requisicao sem o cabecalho recebe um identificador gerado")
    void geraQuandoNaoVem() throws Exception {
        String devolvido = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        assertThat(devolvido)
                .as("toda requisição precisa ser rastreável, tenha o cliente pedido ou não")
                .isNotBlank();
    }

    @Test
    @DisplayName("o identificador do cliente e preservado")
    void preservaOQueVeio() throws Exception {
        String enviado = "11111111-2222-3333-4444-555555555555";

        String devolvido = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.HEADER, enviado))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        // É isto que faz a cadeia continuar entre serviços: quem chama repassa o
        // identificador que recebeu, e os dois lados registram o mesmo valor.
        assertThat(devolvido)
                .as("o identificador recebido precisa seguir adiante, e não ser trocado")
                .isEqualTo(enviado);
    }

    @Test
    @DisplayName("identificador com quebra de linha e descartado")
    void recusaValorPerigoso() throws Exception {
        // O valor vai para o log. Aceitar quebra de linha de fora permitiria
        // forjar uma linha inteira e inventar um evento que nunca aconteceu, que
        // é a falha que o CodeQL apontou no filtro de rate limit.
        String malicioso = "abc\nWARN  Acesso administrativo concedido";

        String devolvido = mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.HEADER, malicioso))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        assertThat(devolvido)
                .as("valor de fora fora do formato precisa ser substituído, não sanitizado pela metade")
                .doesNotContain("\n")
                .doesNotContain("Acesso administrativo");
    }

    @Test
    @DisplayName("o identificador aparece nas linhas de log da requisicao")
    void apareceNoLog() throws Exception {
        String enviado = "99999999-8888-7777-6666-555555555555";

        try (LogCapture log = LogCapture.start()) {
            mockMvc.perform(post("/v1/auth/login")
                    .header(CorrelationIdFilter.HEADER, enviado)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"correlacao@exemplo.test","password":"senha que nao confere"}
                            """));

            // Estar no cabeçalho e não estar no log seria inútil: o objetivo é
            // achar as linhas daquela requisição, não devolver um número bonito.
            assertThat(log.all())
                    .as("as linhas registradas durante a requisição precisam carregar o identificador")
                    .contains(enviado);
        }
    }

    @Test
    @DisplayName("o identificador nao vaza de uma requisicao para a seguinte")
    void naoVazaEntreRequisicoes() throws Exception {
        String primeiro = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

        mockMvc.perform(get("/actuator/health").header(CorrelationIdFilter.HEADER, primeiro));

        // O MDC vive na thread, e a thread volta para o pool. Sem limpeza, a
        // próxima requisição atendida por ela herdaria este identificador e o
        // rastro apontaria para a investigação errada.
        String segundo = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);

        assertThat(segundo)
                .as("cada requisição tem o seu")
                .isNotEqualTo(primeiro);
    }
}
