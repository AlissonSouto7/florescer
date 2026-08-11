package com.florescer.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.florescer.auth.support.AbstractIntegrationTest;
import com.florescer.auth.support.TestTokens;

/**
 * Percorre as rotas do serviço contra cada perfil.
 *
 * <p>A matriz é curta porque o auth-service não tem endpoint protegido próprio:
 * login, registro, JWKS e Swagger são todos públicos. Isso é uma constatação,
 * não uma lacuna do teste. O que dá para verificar aqui é que as rotas públicas
 * seguem alcançáveis por quem não se identificou, e que a regra padrão continua
 * sendo recusar o resto.
 *
 * <p>Quando existir um endpoint protegido de verdade, ele entra nesta lista com
 * as três linhas de sempre: anônimo 401, papel errado 403, papel certo 2xx.
 */
@AutoConfigureMockMvc
class AuthorizationMatrixTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private enum Perfil {
        ANONIMO, BASIC, ADMIN
    }

    private record Caso(String descricao, String rota, Perfil perfil, int esperado) {
        @Override
        public String toString() {
            return descricao;
        }
    }

    static List<Caso> rotasPublicas() {
        return List.of(
                new Caso("JWKS, anônimo -> 200", "/.well-known/jwks.json", Perfil.ANONIMO, 200),
                new Caso("JWKS, BASIC -> 200", "/.well-known/jwks.json", Perfil.BASIC, 200),
                new Caso("OpenAPI, anônimo -> 200", "/v3/api-docs", Perfil.ANONIMO, 200));
    }

    static List<Caso> rotaSemRegraExplicita() {
        return List.of(
                // A regra padrão é authenticated(), então quem não se identificou
                // recebe 401 mesmo numa rota que não existe. É o comportamento
                // desejado: um endpoint novo nasce fechado, e não aberto por
                // esquecimento.
                new Caso("rota qualquer, anônimo -> 401", "/v1/rota-que-nao-existe", Perfil.ANONIMO, 401),
                new Caso("rota qualquer, autenticado -> 404", "/v1/rota-que-nao-existe", Perfil.BASIC, 404));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rotasPublicas")
    @DisplayName("rota publica continua alcancavel sem token")
    void rotaPublicaAlcancavel(Caso caso) throws Exception {
        assertThat(status(caso))
                .as("%s como %s", caso.rota(), caso.perfil())
                .isEqualTo(caso.esperado());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rotaSemRegraExplicita")
    @DisplayName("o padrao e fechado: sem regra explicita, exige identificacao")
    void padraoEFechado(Caso caso) throws Exception {
        assertThat(status(caso))
                .as("%s como %s", caso.rota(), caso.perfil())
                .isEqualTo(caso.esperado());
    }

    @Test
    @DisplayName("registro e login sao alcancaveis sem token")
    void registroELoginSaoPublicos() throws Exception {
        // Sem corpo de propósito: o que interessa é não receber 401. Um 400 já
        // prova que a requisição chegou ao controller.
        assertThat(mockMvc.perform(MockMvcRequestBuilders.post("/v1/auth/login"))
                .andReturn().getResponse().getStatus())
                .as("login não pode exigir token")
                .isNotIn(401, 403);

        assertThat(mockMvc.perform(MockMvcRequestBuilders.post("/v1/auth/register"))
                .andReturn().getResponse().getStatus())
                .as("registro não pode exigir token")
                .isNotIn(401, 403);
    }

    private int status(Caso caso) throws Exception {
        var builder = MockMvcRequestBuilders.get(caso.rota());
        if (caso.perfil() != Perfil.ANONIMO) {
            builder = builder.header("Authorization", "Bearer " + TestTokens.valido("pessoa@exemplo.test"));
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }
}
