package com.florescer.auth.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.florescer.auth.infrastructure.ratelimit.RateLimiter;

@Configuration
@EnableScheduling
public class RateLimitConfig {

    private RateLimiter rateLimiter;

    /**
     * O limite é por endereço de origem e vale para login e registro juntos.
     *
     * <p>O valor precisa separar uso normal de ataque. Alguém que erra a senha
     * algumas vezes seguidas é comum; dezenas de tentativas por minuto não são.
     * Um limite apertado demais transforma esquecimento de senha em bloqueio, e
     * isso também é um problema, só que de outro tipo.
     */
    @Bean
    RateLimiter authRateLimiter(
            @Value("${app.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.rateLimiter = new RateLimiter(maxAttempts, Duration.ofSeconds(windowSeconds));
        return this.rateLimiter;
    }

    /**
     * Limpa janelas expiradas periodicamente.
     *
     * <p>Sem isso, cada endereço que já tentou deixaria uma entrada permanente no
     * mapa, e a proteção contra força bruta viraria um jeito de esgotar a memória
     * do serviço fazendo requisições de endereços variados.
     */
    @Scheduled(fixedDelayString = "${app.rate-limit.cleanup-interval-ms:300000}")
    void evictExpiredWindows() {
        if (rateLimiter != null) {
            rateLimiter.evictExpired();
        }
    }
}
