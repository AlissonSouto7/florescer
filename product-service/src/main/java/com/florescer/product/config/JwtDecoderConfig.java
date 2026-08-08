package com.florescer.product.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Valida os JWTs emitidos pelo auth-service.
 *
 * <p>A chave pública deixou de morar dentro do jar: agora chega por configuração,
 * como PEM (produção, via variável de ambiente) ou como recurso {@code classpath:}
 * ou {@code file:} (desenvolvimento). Empacotar a chave no artefato tornava a troca
 * de chave um redeploy dos dois serviços em sincronia.
 *
 * <p>Além da assinatura e da expiração, o emissor é conferido: uma assinatura válida
 * só prova que a chave é a certa, não que o token veio de quem deveria.
 */
@Configuration
public class JwtDecoderConfig {

    private static final String PEM_PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";

    private final ResourceLoader resourceLoader;
    private final String publicKeyValue;
    private final String issuer;

    public JwtDecoderConfig(ResourceLoader resourceLoader,
                            @Value("${jwt.public-key}") String publicKeyValue,
                            @Value("${jwt.issuer}") String issuer) {
        this.resourceLoader = resourceLoader;
        this.publicKeyValue = publicKeyValue;
        this.issuer = issuer;
    }

    @Bean
    JwtDecoder jwtDecoder() throws IOException {
        RSAPublicKey publicKey;
        try (InputStream pem = openPublicKey()) {
            publicKey = RsaKeyConverters.x509().convert(pem);
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    private InputStream openPublicKey() throws IOException {
        if (publicKeyValue == null || publicKeyValue.isBlank()) {
            throw new IllegalStateException("jwt.public-key nao foi configurada.");
        }

        String trimmed = publicKeyValue.trim();
        if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
            return resourceLoader.getResource(trimmed).getInputStream();
        }

        // Variáveis de ambiente costumam entregar o PEM com \n escapado em vez de quebra real.
        String pem = trimmed.replace("\\n", "\n");
        if (!pem.startsWith(PEM_PUBLIC_HEADER)) {
            throw new IllegalStateException("jwt.public-key deve conter um PEM iniciando com "
                    + PEM_PUBLIC_HEADER + " ou apontar para classpath:/file:");
        }
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    }
}
