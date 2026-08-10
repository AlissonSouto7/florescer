package com.florescer.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * O mesmo problema corrigido no product-service pela issue #10 existe aqui: um
 * handler para {@code Exception} é consultado antes da resolução padrão do
 * Spring, e converte erro do cliente em 500.
 *
 * <p>Os casos foram escritos antes da correção justamente para confirmar quais
 * de fato falham, em vez de assumir que são os mesmos do outro serviço.
 */
@AutoConfigureMockMvc
class ErrorResponseTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("json malformado devolve 400, nao 500")
    void jsonMalformadoDevolve400() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso nao e json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("metodo nao suportado devolve 405, nao 500")
    void metodoNaoSuportadoDevolve405() throws Exception {
        mockMvc.perform(get("/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("corpo ausente devolve 400")
    void corpoAusenteDevolve400() throws Exception {
        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tipo de conteudo nao suportado devolve 415")
    void tipoNaoSuportadoDevolve415() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=alguem"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("erro de validacao devolve os campos como objeto")
    void erroDeValidacaoDevolveCamposComoObjeto() throws Exception {
        // O detalhe precisa ser um objeto JSON navegável por campo, e não o
        // toString de um Map do Java, que nenhum cliente consegue interpretar.
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x","email":"nao-e-email","password":"curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    @DisplayName("rota inexistente responde 401 para quem nao se identificou")
    void rotaInexistenteRespondeNaoAutorizado() throws Exception {
        // Escrevi este caso esperando 404 e a medição mostrou 401. O código está
        // certo e a expectativa é que estava errada: responder 404 revelaria
        // quais rotas existem a quem está sondando a API sem credencial. Só quem
        // se autentica merece saber que o caminho não existe.
        mockMvc.perform(get("/v1/auth/rota-que-nao-existe"))
                .andExpect(status().isUnauthorized());
    }
}
