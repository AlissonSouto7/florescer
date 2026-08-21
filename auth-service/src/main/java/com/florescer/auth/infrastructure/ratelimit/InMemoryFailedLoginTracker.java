package com.florescer.auth.infrastructure.ratelimit;

import java.time.Duration;

/**
 * A contagem de erros por conta, guardada em memória.
 *
 * <p>Reaproveita o {@link RateLimiter}, que já resolve janela deslizante e
 * concorrência por chave, e herda as mesmas limitações escritas lá: reiniciar o
 * serviço zera os contadores, e com mais de uma instância cada uma conta
 * separadamente. Para uma instância, que é o caso, resolve.
 *
 * <p>A chave é a conta pseudonimizada, e não o e-mail. Duas razões, e a segunda
 * é a que importa mais: o e-mail é dado pessoal e não precisa ficar em memória
 * para isto funcionar; e se um dia estes contadores forem parar num log ou num
 * dump, eles não carregam a lista de quem tem conta na loja.
 */
public class InMemoryFailedLoginTracker implements FailedLoginTracker {

    private final RateLimiter contador;

    public InMemoryFailedLoginTracker(int maximoDeErros, Duration janela) {
        this.contador = new RateLimiter(maximoDeErros, janela);
    }

    @Override
    public boolean isBlocked(String account) {
        return !contador.wouldAllow(chave(account));
    }

    @Override
    public void recordFailure(String account) {
        contador.tryAcquire(chave(account));
    }

    @Override
    public void recordSuccess(String account) {
        // Quem entrou provou ser dono da conta: os erros anteriores eram
        // esquecimento de senha, não ataque. Sem isto, errar três vezes e
        // acertar deixaria a conta a três erros do bloqueio pelo resto da
        // janela.
        contador.reset(chave(account));
    }

    @Override
    public long secondsUntilReset(String account) {
        return contador.secondsUntilReset(chave(account));
    }

    /** Normaliza para que "Maria@Loja.com" e "maria@loja.com" sejam a mesma conta. */
    private String chave(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }

    /** Limpa janelas vencidas, para o mapa não crescer sem fim. */
    public void evictExpired() {
        contador.evictExpired();
    }
}
