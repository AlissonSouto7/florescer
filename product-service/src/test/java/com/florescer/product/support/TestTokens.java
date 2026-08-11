package com.florescer.product.support;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Monta tokens que este serviço aceita, sem depender do auth-service.
 *
 * <p>O escopo entra como parâmetro porque é o que decide o papel: o resource
 * server converte cada valor de {@code scope} em uma autoridade {@code ROLE_},
 * e é sobre ela que o {@code @PreAuthorize} decide.
 */
public final class TestTokens {

    private TestTokens() {
    }

    /** Token válido, assinado pela chave que os testes configuram no serviço. */
    public static String comEscopo(String escopo) {
        KeyPair par = RsaTestKeys.keyPair();

        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey((RSAPrivateKey) par.getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .subject("pessoa@exemplo.test")
                .claim("scope", escopo)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
