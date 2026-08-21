package com.florescer.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * O endpoint de saúde e, principalmente, o que ele não pode contar.
 *
 * <p>Publicar o Actuator resolve um problema (o orquestrador precisa saber se o
 * serviço está de pé) e cria outro: os endpoints padrão do Actuator descrevem a
 * configuração inteira da aplicação. {@code /actuator/env} lista variáveis de
 * ambiente, {@code /actuator/beans} lista a estrutura interna, e num serviço de
 * autenticação isso é um mapa para quem quiser atacá-lo.
 */
@AutoConfigureMockMvc
class HealthEndpointTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health responde sem autenticacao")
    void healthEPublico() throws Exception {
        // Quem consulta é o orquestrador do container, que não tem credencial.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("liveness e readiness respondem separadamente")
    void sondasSeparadas() throws Exception {
        // São perguntas diferentes: liveness é "reiniciar resolve", readiness é
        // "esperar resolve". Reiniciar um serviço que só aguardava o banco troca
        // uma espera por uma interrupção.
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("health nao revela detalhe interno")
    void healthNaoDetalha() throws Exception {
        String corpo = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .as("o detalhe do health nomeia componentes, versão de banco e motivo da falha")
                .doesNotContain("components", "db", "diskSpace", "database");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/env", "/actuator/beans", "/actuator/configprops",
            "/actuator/loggers", "/actuator/mappings", "/actuator/metrics", "/actuator/threaddump",
            "/actuator/heapdump", "/actuator"})
    @DisplayName("nenhum outro endpoint do actuator esta exposto")
    void demaisEndpointsNaoExpostos(String rota) throws Exception {
        int status = mockMvc.perform(get(rota)).andReturn().getResponse().getStatus();

        // 404 porque não foi publicado, ou 401 porque a regra padrão exige
        // identificação. Qualquer um dos dois serve; 200 não.
        assertThat(status)
                .as("%s não pode responder conteúdo", rota)
                .isIn(401, 404);
    }
}
