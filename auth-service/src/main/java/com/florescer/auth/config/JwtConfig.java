package com.florescer.auth.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Carrega o par de chaves RSA usado para assinar e validar os JWTs.
 *
 * <p>Cada chave pode ser fornecida de duas formas, decididas pelo prefixo do valor:
 * <ul>
 *   <li>{@code classpath:} ou {@code file:} apontam para um recurso (uso em desenvolvimento);</li>
 *   <li>qualquer outro valor é tratado como o conteúdo PEM em si, o formato usado em
 *       produção, onde a chave chega por variável de ambiente ou secret manager.</li>
 * </ul>
 */
@Configuration
public class JwtConfig {

    private static final String PEM_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";

    private final ResourceLoader resourceLoader;
    private final String privateKeyValue;
    private final String publicKeyValue;

    public JwtConfig(ResourceLoader resourceLoader,
                     @Value("${jwt.private-key}") String privateKeyValue,
                     @Value("${jwt.public-key}") String publicKeyValue) {
        this.resourceLoader = resourceLoader;
        this.privateKeyValue = privateKeyValue;
        this.publicKeyValue = publicKeyValue;
    }

    @Bean
    RSAPrivateKey privateKey() throws IOException {
        try (InputStream pem = openKey(privateKeyValue, PEM_PRIVATE_HEADER, "jwt.private-key")) {
            return RsaKeyConverters.pkcs8().convert(pem);
        }
    }

    @Bean
    RSAPublicKey publicKey() throws IOException {
        try (InputStream pem = openKey(publicKeyValue, PEM_PUBLIC_HEADER, "jwt.public-key")) {
            return RsaKeyConverters.x509().convert(pem);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private InputStream openKey(String value, String expectedPemHeader, String propertyName) throws IOException {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " nao foi configurada.");
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
            return resourceLoader.getResource(trimmed).getInputStream();
        }

        // Variáveis de ambiente costumam entregar o PEM com \n escapado em vez de quebra real.
        String pem = trimmed.replace("\\n", "\n");
        if (!pem.startsWith(expectedPemHeader)) {
            throw new IllegalStateException(propertyName + " deve conter um PEM iniciando com "
                    + expectedPemHeader + " ou apontar para classpath:/file:");
        }
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    }
}
