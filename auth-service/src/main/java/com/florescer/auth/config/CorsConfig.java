package com.florescer.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Permite que o frontend, servido de outra origem, chame esta API.
 *
 * <p>O navegador aplica a política de mesma origem: uma página em
 * {@code http://localhost:3000} não recebe a resposta de
 * {@code http://localhost:8080} a menos que o servidor declare que aceita
 * aquela origem. Sem isso a requisição chega a ser enviada, mas a resposta é
 * descartada antes de o JavaScript vê-la, e o erro aparece só no console.
 *
 * <p>As origens vêm de configuração, e não há curinga: {@code *} combinado com
 * credenciais é recusado pela própria especificação, e mesmo sem credenciais
 * abriria a API para qualquer site pedir em nome de quem estiver navegando.
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // O token vai no cabeçalho Authorization, não em cookie, então não há
        // credencial de navegador para permitir.
        configuration.setAllowCredentials(false);
        // Evita repetir o preflight a cada chamada durante a navegação.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
