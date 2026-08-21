package com.florescer.product.api;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

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
 * As restrições declaradas nos DTOs precisam valer de verdade.
 *
 * <p>Anotação de validação que não é executada é pior que ausência de validação:
 * o código aparenta estar protegido, e a revisão passa batido porque as
 * anotações estão lá.
 */
@AutoConfigureMockMvc
class ProductValidationTest extends AbstractIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("preco negativo e recusado")
    void precoNegativoERecusado() throws Exception {
        String json = """
                {"name":"Rosa","type":"Flor","description":"Bonita","price":-10.0,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;

        mockMvc.perform(criarProduto(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("estoque negativo e recusado")
    void estoqueNegativoERecusado() throws Exception {
        String json = """
                {"name":"Rosa","type":"Flor","description":"Bonita","price":10.0,
                 "quantityStock":-5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;

        mockMvc.perform(criarProduto(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nome em branco e recusado")
    void nomeEmBrancoERecusado() throws Exception {
        String json = """
                {"name":"   ","type":"Flor","description":"Bonita","price":10.0,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;

        mockMvc.perform(criarProduto(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nome acima do limite de tamanho e recusado")
    void nomeAcimaDoLimiteERecusado() throws Exception {
        String nomeGigante = "a".repeat(300);
        String json = """
                {"name":"%s","type":"Flor","description":"Bonita","price":10.0,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """.formatted(nomeGigante);

        mockMvc.perform(criarProduto(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("produto valido e aceito")
    void produtoValidoEAceito() throws Exception {
        String json = """
                {"name":"Rosa vermelha","type":"Flor","description":"Bonita","price":29.9,
                 "quantityStock":5,"careRequirements":"Regar","availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """;

        mockMvc.perform(criarProduto(json))
                .andExpect(status().isCreated());
    }

    // O tipo concreto, e nao o pai: no Spring Framework 7 o construtor de
    // multipart deixou de ser um MockHttpServletRequestBuilder.
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder criarProduto(String json) {
        MockMultipartFile produto = new MockMultipartFile(
                "product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile imagem = new MockMultipartFile(
                "image", "rosa.png", MediaType.IMAGE_PNG_VALUE, PNG);

        return multipart("/v1/product")
                .file(produto)
                .file(imagem)
                .header("Authorization", "Bearer " + tokenAdmin());
    }

    private String tokenAdmin() {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) RsaTestKeys.keyPair().getPublic())
                .privateKey((RSAPrivateKey) RsaTestKeys.keyPair().getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("admin@florescer.test")
                .claim("scope", "ADMIN")
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
