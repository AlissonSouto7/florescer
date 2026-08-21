package com.florescer.product.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.RsaTestKeys;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * O código de status precisa dizer a verdade sobre o que aconteceu.
 *
 * <p>Um 500 devolvido para um pedido malformado mente duas vezes: diz ao cliente
 * que ele não tem o que corrigir, e diz a quem opera o sistema que há um defeito
 * onde não há. Também some com o erro real do log de monitoramento, porque tudo
 * vira a mesma linha genérica.
 */
@AutoConfigureMockMvc
class ErrorResponseTest extends AbstractIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("json malformado devolve 400, nao 500")
    void jsonMalformadoDevolve400() throws Exception {
        MockMultipartFile produto = new MockMultipartFile(
                "product", "", MediaType.APPLICATION_JSON_VALUE, "{isso nao e json}".getBytes());
        MockMultipartFile imagem = new MockMultipartFile(
                "image", "rosa.png", MediaType.IMAGE_PNG_VALUE, PNG);

        mockMvc.perform(multipart("/v1/product")
                        .file(produto).file(imagem)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("valor fora do enum devolve 400, nao 500")
    void valorForaDoEnumDevolve400() throws Exception {
        String json = """
                {"name":"Rosa","type":"Flor","description":"Bonita","price":10.0,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"NAO_EXISTE"}
                """;
        MockMultipartFile produto = new MockMultipartFile(
                "product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
        MockMultipartFile imagem = new MockMultipartFile(
                "image", "rosa.png", MediaType.IMAGE_PNG_VALUE, PNG);

        mockMvc.perform(multipart("/v1/product")
                        .file(produto).file(imagem)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("uuid malformado na rota devolve 400, nao 500")
    void uuidMalformadoDevolve400() throws Exception {
        mockMvc.perform(get("/v1/product/nao-e-um-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("metodo nao suportado devolve 405, nao 500")
    void metodoNaoSuportadoDevolve405() throws Exception {
        mockMvc.perform(post("/v1/product/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("parte obrigatoria ausente devolve 400, nao 500")
    void parteObrigatoriaAusenteDevolve400() throws Exception {
        MockMultipartFile imagem = new MockMultipartFile(
                "image", "rosa.png", MediaType.IMAGE_PNG_VALUE, PNG);

        mockMvc.perform(multipart("/v1/product")
                        .file(imagem)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cliente anonimo em rota protegida recebe 401, nao 403")
    void anonimoRecebe401() throws Exception {
        // 401 diz "identifique-se"; 403 diz "eu sei quem você é e não pode".
        // Responder 403 a quem nunca se identificou tira do cliente a informação
        // de que bastava enviar credenciais.
        mockMvc.perform(delete("/v1/product/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("resposta de acesso negado tambem e json")
    void acessoNegadoRespondeJson() throws Exception {
        mockMvc.perform(delete("/v1/product/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenBasic()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    private String tokenAdmin() {
        return token("ADMIN");
    }

    private String tokenBasic() {
        return token("BASIC");
    }

    private String token(String scope) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) RsaTestKeys.keyPair().getPublic())
                .privateKey((RSAPrivateKey) RsaTestKeys.keyPair().getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("usuario@florescer.test")
                .claim("scope", scope)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
