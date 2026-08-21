package com.florescer.product.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.FakeJwksServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;

/**
 * Prova que trocar a chave do auth-service não exige reiniciar este serviço.
 *
 * <p>Era esse o ponto de adotar JWKS. Com a chave pública vindo de configuração,
 * uma troca só valia depois de atualizar e reiniciar todo mundo, e qualquer
 * defasagem entre os serviços recusava todos os tokens no intervalo.
 *
 * <p>Os casos rodam em ordem porque o segundo depende do estado deixado pelo
 * primeiro: a rotação acontece com o contexto no ar, e é justamente isso que
 * está sob teste.
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwksRotationTest extends AbstractIntegrationTest {

    private static final FakeJwksServer AUTH_FALSO = FakeJwksServer.start();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registry) {
        registry.add("jwt.jwks-uri", AUTH_FALSO::jwksUri);
    }

    @AfterAll
    static void encerrar() {
        AUTH_FALSO.close();
    }

    @Test
    @Order(1)
    @DisplayName("token assinado pela chave publicada e aceito")
    void chaveAtualEAceita() throws Exception {
        criarProduto(tokenAssinadoPor(AUTH_FALSO.parAtual()))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("token da chave nova e aceito sem reiniciar o servico")
    void chaveRotacionadaEAceitaSemReiniciar() throws Exception {
        KeyPair anterior = AUTH_FALSO.parAtual();

        AUTH_FALSO.rotacionar();

        // O contexto Spring é o mesmo do caso anterior: nada foi reiniciado entre
        // uma chamada e outra, só o conteúdo publicado pelo auth mudou.
        criarProduto(tokenAssinadoPor(AUTH_FALSO.parAtual()))
                .andExpect(status().isCreated());

        // E a chave que saiu de circulação deixa de valer, senão "aceitar a nova"
        // seria compatível com "aceitar qualquer uma".
        criarProduto(tokenAssinadoPor(anterior))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions criarProduto(String token) throws Exception {
        MockMultipartFile produto = new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                """
                {"name":"Samambaia","type":"Planta","description":"Verde e viçosa",
                 "price":49.90,"quantityStock":3,"careRequirements":"Meia sombra",
                 "availability":true,"status":"ATIVO",
                 "heightCm":40,"light":"MEIA_SOMBRA","watering":"SEMANAL","petSafe":true,
                 "environment":"INTERNO","difficulty":"FACIL","includesPot":true}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockMultipartFile imagem = new MockMultipartFile("image", "planta.png", MediaType.IMAGE_PNG_VALUE,
                pngMinimo());

        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/v1/product")
                .file(produto)
                .file(imagem)
                .header("Authorization", "Bearer " + token));
    }

    private String tokenAssinadoPor(KeyPair par) throws JOSEException {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey((RSAPrivateKey) par.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyIDFromThumbprint()
                .build();

        Instant agora = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("auth-service")
                .subject("admin@exemplo.test")
                .issueTime(Date.from(agora))
                .expirationTime(Date.from(agora.plusSeconds(3600)))
                .claim("scope", "ADMIN")
                .build();

        // O kid no cabeçalho é o que faz quem valida procurar a chave certa no
        // conjunto publicado, e recarregar o conjunto quando não a encontra.
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwk.getKeyID()).build(),
                claims);
        jwt.sign(new RSASSASigner(jwk.toPrivateKey()));
        return jwt.serialize();
    }

    /** PNG de 1x1 válido: o upload confere os primeiros bytes do arquivo. */
    private byte[] pngMinimo() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
                        + "YPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    }
}
