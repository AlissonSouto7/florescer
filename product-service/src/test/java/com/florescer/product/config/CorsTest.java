package com.florescer.product.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;

/**
 * Sem estes cabeçalhos, o navegador descarta a resposta antes de o JavaScript
 * vê-la, e a vitrine não consegue listar produto algum.
 *
 * <p>Estes testes falham quando o bean {@code CorsConfigurationSource} deixa de
 * existir, que é a forma pela qual a configuração se perderia de verdade. O
 * comportamento foi verificado removendo o bean: o preflight passa a responder
 * 403 e o cabeçalho some.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
class CorsTest extends AbstractIntegrationTest {

    private static final String ORIGEM_PERMITIDA = "http://localhost:3000";
    private static final String ORIGEM_DESCONHECIDA = "http://site-de-terceiro.test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("preflight de origem permitida e aceito sem token")
    void preflightDeOrigemPermitidaEAceito() throws Exception {
        mockMvc.perform(options("/v1/product")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("preflight de origem desconhecida e recusado")
    void preflightDeOrigemDesconhecidaERecusado() throws Exception {
        mockMvc.perform(options("/v1/product")
                        .header("Origin", ORIGEM_DESCONHECIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a vitrine anonima recebe o cabecalho que o navegador exige")
    void vitrineAnonimaRecebeCabecalho() throws Exception {
        mockMvc.perform(get("/v1/product").header("Origin", ORIGEM_PERMITIDA))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("origem desconhecida nao recebe liberacao")
    void origemDesconhecidaNaoRecebeLiberacao() throws Exception {
        mockMvc.perform(get("/v1/product").header("Origin", ORIGEM_DESCONHECIDA))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
