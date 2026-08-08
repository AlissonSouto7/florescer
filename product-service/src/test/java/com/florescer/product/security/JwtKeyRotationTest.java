package com.florescer.product.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
 * Prova que a rotação de chave surte efeito.
 *
 * <p>Rotacionar uma chave só serve se os tokens assinados pela chave antiga
 * pararem de ser aceitos. Sem esta verificação, a rotação seria um gesto sem
 * consequência: a chave nova entra, a antiga continua abrindo a porta.
 *
 * <p>O serviço sobe confiando na chave pública de {@link RsaTestKeys}. Cada teste
 * forja um token com outra chave e espera recusa.
 */
@AutoConfigureMockMvc
class JwtKeyRotationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejeitaTokenAssinadoPorChaveQueNaoEaConfigurada() throws Exception {
        String tokenDaChaveAntiga = tokenAssinadoPor(gerarOutroPar(), "ADMIN");

        mockMvc.perform(delete("/v1/product/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDaChaveAntiga))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aceitaTokenAssinadoPelaChaveConfigurada() throws Exception {
        String token = tokenAssinadoPor(RsaTestKeys.keyPair(), "ADMIN");

        // Não é 401: a autenticação passou. O produto sorteado não existe, então
        // a resposta esperada é 404, e é isso que separa "token recusado" de
        // "token aceito e recurso ausente".
        mockMvc.perform(delete("/v1/product/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaTokenSemAssinaturaValidaEmRotaProtegida() throws Exception {
        mockMvc.perform(delete("/v1/product/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized());
    }

    private String tokenAssinadoPor(KeyPair keyPair, String scope) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("alguem@florescer.com")
                .claim("scope", scope)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private KeyPair gerarOutroPar() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
