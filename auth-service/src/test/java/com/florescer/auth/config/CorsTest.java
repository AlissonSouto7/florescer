package com.florescer.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * Sem estes cabeçalhos, o navegador descarta a resposta antes de o JavaScript
 * vê-la, e o frontend não consegue falar com a API.
 *
 * <p>O caso do preflight é o que mais engana: o navegador envia um OPTIONS sem
 * token antes da requisição real. Se a cadeia de segurança tratar esse OPTIONS
 * como requisição comum, ele é recusado por falta de autenticação e a chamada
 * verdadeira nunca acontece.
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
        mockMvc.perform(options("/v1/auth/login")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("preflight de origem desconhecida e recusado")
    void preflightDeOrigemDesconhecidaERecusado() throws Exception {
        mockMvc.perform(options("/v1/auth/login")
                        .header("Origin", ORIGEM_DESCONHECIDA)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("resposta a origem permitida traz o cabecalho que o navegador exige")
    void respostaTrazCabecalhoParaOrigemPermitida() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .header("Origin", ORIGEM_PERMITIDA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qualquer@exemplo.test","password":"senha-errada"}
                                """))
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_PERMITIDA));
    }

    @Test
    @DisplayName("resposta a origem desconhecida nao libera acesso")
    void respostaNaoLiberaOrigemDesconhecida() throws Exception {
        // Sem o cabeçalho, o navegador descarta a resposta mesmo que o servidor
        // a tenha processado. É o que impede outro site de ler dados da API em
        // nome de quem está navegando.
        mockMvc.perform(post("/v1/auth/login")
                        .header("Origin", ORIGEM_DESCONHECIDA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qualquer@exemplo.test","password":"senha-errada"}
                                """))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
