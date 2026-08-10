package com.florescer.auth.infrastructure.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita quantas tentativas uma mesma origem pode fazer numa janela de tempo.
 *
 * <p>Usa janela deslizante por contagem simples: cada chave guarda quando a
 * janela atual começou e quantas tentativas houve nela. Passada a janela, a
 * contagem recomeça.
 *
 * <p>O estado vive em memória, o que traz duas limitações que precisam ser ditas
 * em voz alta: reiniciar o serviço zera os contadores, e com mais de uma
 * instância cada uma conta separadamente, então o limite efetivo é multiplicado
 * pelo número de instâncias. Para uma instância só, que é o caso hoje, resolve.
 * Com várias, o contador precisa ser compartilhado, tipicamente em Redis.
 */
public class RateLimiter {

    private record Window(Instant startedAt, int attempts) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public RateLimiter(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    /**
     * Registra uma tentativa e diz se ela deve ser permitida.
     *
     * <p>O cálculo é atômico por chave: sem isso, requisições concorrentes da
     * mesma origem leriam o mesmo contador e passariam todas, que é exatamente o
     * cenário de um ataque automatizado.
     */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();

        Window updated = windows.compute(key, (k, current) -> {
            if (current == null || now.isAfter(current.startedAt().plus(window))) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.attempts() + 1);
        });

        return updated.attempts() <= maxAttempts;
    }

    /**
     * Quantos segundos faltam para a janela da chave expirar. Serve para
     * responder {@code Retry-After}, que diz ao cliente quando voltar em vez de
     * deixá-lo tentando.
     */
    public long secondsUntilReset(String key) {
        Window current = windows.get(key);
        if (current == null) {
            return 0;
        }
        long remaining = Duration.between(Instant.now(), current.startedAt().plus(window)).getSeconds();
        return Math.max(remaining, 0);
    }

    /**
     * Descarta janelas já expiradas.
     *
     * <p>Sem isso o mapa cresceria indefinidamente com uma entrada por endereço
     * que já tentou, o que transformaria a proteção contra força bruta numa forma
     * de esgotar a memória do serviço.
     */
    public void evictExpired() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(e -> now.isAfter(e.getValue().startedAt().plus(window)));
    }

    /**
     * Quantas origens estão sendo acompanhadas no momento.
     *
     * <p>É o número que revela se a limpeza está funcionando: crescimento sem
     * queda significa janelas acumulando em memória.
     */
    public int trackedKeys() {
        return windows.size();
    }

    /**
     * Libera imediatamente uma origem bloqueada.
     *
     * <p>Existe porque bloqueio por engano acontece: alguém esquece a senha,
     * erra várias vezes e fica preso pelo resto da janela. Sem uma forma de
     * liberar, a saída seria reiniciar o serviço.
     */
    public void reset(String key) {
        windows.remove(key);
    }

    /** Descarta todas as janelas. */
    public void clear() {
        windows.clear();
    }
}
