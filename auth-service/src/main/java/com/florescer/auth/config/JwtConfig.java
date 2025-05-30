package com.florescer.auth.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtConfig {

    @Bean
    RSAPrivateKey privateKey() throws Exception {
        String privateKeyPEM = Files.readString(Path.of("src/main/resources/chaves/app.key"))
                                    .replace("-----BEGIN PRIVATE KEY-----", "")
                                    .replace("-----END PRIVATE KEY-----", "")
                                    .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    @Bean
    RSAPublicKey publicKey() throws Exception {
        String publicKeyPEM = Files.readString(Path.of("src/main/resources/chaves/app.pub"))
                                   .replace("-----BEGIN PUBLIC KEY-----", "")
                                   .replace("-----END PUBLIC KEY-----", "")
                                   .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
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
}
