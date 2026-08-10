package com.florescer.product.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;

/**
 * A listagem é pública e recebia três parâmetros direto do cliente.
 *
 * <p>Sem teto, {@code ?size=1000000} obriga banco e serialização a materializar
 * tudo numa requisição, e basta repetir a chamada para derrubar o serviço. Sem
 * lista de campos permitidos, ordenar por um nome inexistente virava 500.
 */
@AutoConfigureMockMvc
class PaginationLimitsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("tamanho de pagina acima do teto e recusado")
    void tamanhoAcimaDoTetoERecusado() throws Exception {
        mockMvc.perform(get("/v1/product").param("size", "1000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("tamanho dentro do teto e aceito")
    void tamanhoDentroDoTetoEAceito() throws Exception {
        mockMvc.perform(get("/v1/product").param("size", "50"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ordenar por campo inexistente devolve 400, nao 500")
    void ordenarPorCampoInexistenteDevolve400() throws Exception {
        // Antes o nome ia direto para a consulta e explodia lá dentro, o que
        // além do 500 revelava detalhe interno na mensagem.
        mockMvc.perform(get("/v1/product").param("sort", "campoQueNaoExiste"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ordenar por campo permitido funciona, inclusive descendente")
    void ordenarPorCampoPermitidoFunciona() throws Exception {
        mockMvc.perform(get("/v1/product").param("sort", "price,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ordenar por dois campos funciona")
    void ordenarPorDoisCamposFunciona() throws Exception {
        mockMvc.perform(get("/v1/product").param("sort", "status", "price,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("direcao sem campo antes e recusada")
    void direcaoSemCampoAntesERecusada() throws Exception {
        mockMvc.perform(get("/v1/product").param("sort", "desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("pagina negativa e recusada")
    void paginaNegativaERecusada() throws Exception {
        mockMvc.perform(get("/v1/product").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tamanho zero e recusado")
    void tamanhoZeroERecusado() throws Exception {
        mockMvc.perform(get("/v1/product").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem parametros usa os valores padrao")
    void semParametrosUsaPadrao() throws Exception {
        mockMvc.perform(get("/v1/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10));
    }
}
