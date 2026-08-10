package com.florescer.auth.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
 * Monta tokens à mão para os testes de segurança.
 *
 * <p>Os tokens não saem do endpoint de login de propósito: o que interessa
 * verificar são justamente os casos que o login nunca produz, como token
 * expirado ou assinado por outra chave.
 */
public final class TestTokens {

    private TestTokens() {
    }

    /** Token que a aplicação aceita: chave certa, dentro da validade. */
    public static String valido(String subject) {
        return assinado(RsaTestKeys.keyPair(), subject, Instant.now(), 3600);
    }

    /** Token bem formado e assinado pela chave certa, mas fora da validade. */
    public static String expirado(String subject) {
        Instant passado = Instant.now().minusSeconds(7200);
        return assinado(RsaTestKeys.keyPair(), subject, passado, 60);
    }

    /** Token assinado por uma chave que a aplicação não conhece. */
    public static String deOutraChave(String subject) {
        return assinado(novoPar(), subject, Instant.now(), 3600);
    }

    private static String assinado(KeyPair par, String subject, Instant emitidoEm, long duracaoSegundos) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey((RSAPrivateKey) par.getPrivate())
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plusSeconds(duracaoSegundos))
                .subject(subject)
                .claim("scope", "ADMIN")
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private static KeyPair novoPar() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar o par RSA de teste", ex);
        }
    }
}
