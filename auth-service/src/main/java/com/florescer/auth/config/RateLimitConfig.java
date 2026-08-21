package com.florescer.auth.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.florescer.auth.infrastructure.ratelimit.ClientResolver;
import com.florescer.auth.infrastructure.ratelimit.InMemoryFailedLoginTracker;
import com.florescer.auth.infrastructure.ratelimit.RateLimiter;

@Configuration
@EnableScheduling
public class RateLimitConfig {

    private RateLimiter rateLimiter;
    private InMemoryFailedLoginTracker failedLoginTracker;

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
     * Quem é o cliente, atrás do proxy.
     *
     * <p>O site é o único que fala com este serviço em produção, então sem esta
     * configuração toda tentativa chega do mesmo endereço e divide a mesma cota:
     * dez senhas erradas de um desconhecido trancavam a vendedora.
     *
     * <p>{@code trusted-proxies} vazio mantém o comportamento antigo, que é o
     * correto para quem expõe o serviço direto. {@code trusted-hops} é quantos
     * proxies existem no caminho: 1 com o site na frente, 2 quando há um túnel
     * ou CDN antes dele.
     */
    @Bean
    ClientResolver clientResolver(
            @Value("${app.rate-limit.trusted-proxies:}") String proxiesConfiaveis,
            @Value("${app.rate-limit.trusted-hops:1}") int saltosConfiaveis) {

        Set<String> proxies = Arrays.stream(proxiesConfiaveis.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .collect(Collectors.toSet());

        return new ClientResolver(proxies, saltosConfiaveis);
    }

    /**
     * A contagem de erros de senha por conta.
     *
     * <p>Janela mais longa que a do limite por origem, e teto parecido: aqui o
     * que se mede não é "quantas requisições este endereço fez", é "quantas
     * vezes erraram a senha desta conta". Errar cinco vezes em quinze minutos é
     * esquecimento; errar vinte é alguém tentando adivinhar.
     */
    @Bean
    InMemoryFailedLoginTracker failedLoginTracker(
            @Value("${app.login-attempts.max-failures:10}") int maximoDeErros,
            @Value("${app.login-attempts.window-seconds:900}") long janelaEmSegundos) {
        this.failedLoginTracker = new InMemoryFailedLoginTracker(maximoDeErros, Duration.ofSeconds(janelaEmSegundos));
        return this.failedLoginTracker;
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
        if (failedLoginTracker != null) {
            failedLoginTracker.evictExpired();
        }
    }
}
