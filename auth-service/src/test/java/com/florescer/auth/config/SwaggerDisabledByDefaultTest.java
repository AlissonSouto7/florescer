package com.florescer.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * A documentação viva não pode subir ligada sem ninguém pedir.
 *
 * <p>Ela lista cada rota, cada campo e quem precisa de token: é o mapa do
 * sistema pronto para quem está procurando por onde entrar. Em desenvolvimento
 * vale muito, e por isso o compose de lá liga explicitamente.
 *
 * <p>Este teste existe porque o padrão é o que decide o que acontece quando
 * alguém esquece. Ligado por padrão, esquecer publica tudo em silêncio;
 * desligado, o custo de esquecer é um Swagger que não abre em desenvolvimento,
 * que aparece na hora e se resolve numa linha.
 *
 * <p><b>Sem `@TestPropertySource` de propósito</b>: o que está sob teste é
 * justamente o valor que vale quando ninguém configura nada.
 */
@AutoConfigureMockMvc
class SwaggerDisabledByDefaultTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("sem configuração, o contrato OpenAPI não existe")
    void openApiNaoExiste() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem configuração, a interface do Swagger não existe")
    void swaggerUiNaoExiste() throws Exception {
        // 404 e não 401: a rota some, em vez de existir protegida. Rota
        // protegida ainda confirma que o serviço tem Swagger.
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
