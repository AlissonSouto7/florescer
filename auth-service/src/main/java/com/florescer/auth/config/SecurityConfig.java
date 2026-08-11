package com.florescer.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.filter.CorsFilter;

import com.florescer.auth.infrastructure.ratelimit.RateLimitFilter;
import com.florescer.auth.infrastructure.ratelimit.RateLimiter;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@SecurityScheme(name = SecurityConfig.SECURITY, type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class SecurityConfig {

	public static final String SECURITY = "bearerAuth";
	private final RateLimiter authRateLimiter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
				// Explícito por clareza: o Spring Security já aplica CORS sozinho
				// quando existe um bean CorsConfigurationSource, e verificar isso
				// exigiria ler a configuração interna do framework. Deixar visível
				// evita que alguém remova o bean sem perceber que a cadeia depende
				// dele. Ver CorsConfig.
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(
						auth -> auth.requestMatchers("/v1/auth/register", "/v1/auth/login", "/v3/api-docs/**",
								"/swagger-ui/**", "/swagger-ui.html", "/swagger",
								// Precisa ser público por definição: quem valida um
								// token ainda não tem token para se identificar.
								// Só material público trafega aqui, ver JwksController.
								"/.well-known/jwks.json",
								// Health precisa ser publico porque quem consulta e o
								// orquestrador do container, que nao tem credencial. So
								// diz UP ou DOWN: o detalhe esta desligado no yml.
								"/actuator/health", "/actuator/health/**")
								.permitAll().anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Antes de tudo: uma tentativa recusada não deve custar consulta
				// ao banco nem verificação de BCrypt, que é cara de propósito.
				.addFilterBefore(new RateLimitFilter(authRateLimiter), CorsFilter.class)
				.build();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}