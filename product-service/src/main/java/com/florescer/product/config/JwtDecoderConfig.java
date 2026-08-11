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
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Valida os JWTs emitidos pelo auth-service.
 *
 * <p>Há duas formas de dizer onde está a chave, e {@code jwt.jwks-uri} tem
 * precedência:
 * <ul>
 *   <li><b>{@code jwt.jwks-uri}</b>: aponta para o {@code /.well-known/jwks.json}
 *       do auth-service. A chave é buscada pelo {@code kid} do token, então uma
 *       troca de chave lá vale aqui sem redeploy nem reinício.</li>
 *   <li><b>{@code jwt.public-key}</b>: uma chave fixa em PEM, por variável de
 *       ambiente ou recurso {@code classpath:}/{@code file:}. Continua disponível
 *       porque o modo JWKS exige alcançar o auth-service pela rede, o que nem
 *       todo ambiente permite. Nesse modo não existe rotação sem redeploy.</li>
 * </ul>
 *
 * <p>Além da assinatura e da expiração, o emissor é conferido: uma assinatura válida
 * só prova que a chave é a certa, não que o token veio de quem deveria.
 */
@Configuration
public class JwtDecoderConfig {

    private static final String PEM_PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";

    private final ResourceLoader resourceLoader;
    private final String publicKeyValue;
    private final String jwkSetUri;
    private final String issuer;

    public JwtDecoderConfig(ResourceLoader resourceLoader,
                            @Value("${jwt.public-key:}") String publicKeyValue,
                            @Value("${jwt.jwks-uri:}") String jwkSetUri,
                            @Value("${jwt.issuer}") String issuer) {
        this.resourceLoader = resourceLoader;
        this.publicKeyValue = publicKeyValue;
        this.jwkSetUri = jwkSetUri;
        this.issuer = issuer;
    }

    @Bean
    JwtDecoder jwtDecoder() throws IOException {
        NimbusJwtDecoder decoder = hasJwkSetUri() ? decoderFromJwks() : decoderFromStaticKey();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    /**
     * Busca a chave no auth-service, pelo {@code kid} do cabeçalho do token.
     *
     * <p>Uma chave desconhecida faz o cliente do Nimbus recarregar o conjunto, o
     * que é o mecanismo que torna a rotação possível sem reiniciar este serviço.
     */
    private NimbusJwtDecoder decoderFromJwks() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri.trim())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    /**
     * Modo antigo, mantido porque a troca para JWKS depende de o auth-service
     * estar acessível a partir daqui, o que nem todo ambiente garante. Com uma
     * chave fixa não há rotação sem redeploy.
     */
    private NimbusJwtDecoder decoderFromStaticKey() throws IOException {
        RSAPublicKey publicKey;
        try (InputStream pem = openPublicKey()) {
            publicKey = RsaKeyConverters.x509().convert(pem);
        }
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private boolean hasJwkSetUri() {
        return jwkSetUri != null && !jwkSetUri.isBlank();
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
