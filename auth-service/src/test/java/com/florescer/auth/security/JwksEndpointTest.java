package com.florescer.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * O endpoint JWKS é público e serve material de chave, então a verificação mais
 * importante aqui não é o que ele publica, e sim o que ele não pode publicar.
 */
@AutoConfigureMockMvc
class JwksEndpointTest extends AbstractIntegrationTest {

    /** Os parâmetros que só existem na chave privada, segundo a RFC 7518. */
    private static final String[] CAMPOS_PRIVADOS = {"d", "p", "q", "dp", "dq", "qi"};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("o endpoint e alcancavel sem autenticacao")
    void publicoSemToken() throws Exception {
        // Quem precisa da chave está justamente tentando validar um token, então
        // exigir token aqui seria circular.
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty());
    }

    @Test
    @DisplayName("nenhum parametro da chave privada e publicado")
    void naoVazaChavePrivada() throws Exception {
        String corpo = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn().getResponse().getContentAsString();

        JsonNode chave = objectMapper.readTree(corpo).get("keys").get(0);

        for (String campo : CAMPOS_PRIVADOS) {
            assertThat(chave.has(campo))
                    .as("o parâmetro privado '%s' não pode aparecer num endpoint público", campo)
                    .isFalse();
        }
        assertThat(corpo)
                .as("nem o PEM da chave privada, em nenhuma forma")
                .doesNotContain("PRIVATE KEY");
    }

    @Test
    @DisplayName("o kid do token emitido corresponde ao kid publicado")
    void kidDoTokenBateComOPublicado() throws Exception {
        String jwks = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn().getResponse().getContentAsString();
        String kidPublicado = objectMapper.readTree(jwks).get("keys").get(0).get("kid").asText();

        String kidDoToken = cabecalhoDoToken(tokenDeUmLoginReal()).get("kid").asText();

        // Sem esta correspondência o JWKS não serve para nada: quem valida procura
        // pelo kid do cabeçalho e não encontraria a chave.
        assertThat(kidDoToken)
                .as("o token precisa apontar para uma chave que o JWKS publica")
                .isEqualTo(kidPublicado);
    }

    private String tokenDeUmLoginReal() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Pessoa","email":"jwks@exemplo.test","password":"uma senha bem comprida"}
                        """));

        String resposta = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"jwks@exemplo.test","password":"uma senha bem comprida"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resposta).get("accessToken").asText();
    }

    private JsonNode cabecalhoDoToken(String token) throws Exception {
        String cabecalho = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        return objectMapper.readTree(cabecalho);
    }
}
