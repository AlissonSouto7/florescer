package com.florescer.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.infrastructure.ratelimit.RateLimiter;
import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * Sem limite de tentativas, descobrir uma senha depende apenas de tempo de
 * máquina, e existe uma conta administrativa com endereço previsível.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.max-attempts=5",
        "app.rate-limit.window-seconds=60"
})
class RateLimitTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiter rateLimiter;

    /**
     * O contador é um bean compartilhado por toda a classe, e a janela dura mais
     * que a suíte: sem limpar, a cota consumida por um teste sobra para o
     * seguinte, e a ordem de execução passa a mudar o resultado.
     */
    @BeforeEach
    void limparContadores() {
        rateLimiter.clear();
    }

    @Test
    @DisplayName("tentativas acima do limite recebem 429 com Retry-After")
    void excedendoOLimiteRecebe429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login("tentativa" + i + "@exemplo.test"));
        }

        mockMvc.perform(login("tentativa6@exemplo.test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("a sexta tentativa na janela precisa ser recusada")
                        .isEqualTo(429))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER))
                        .as("o cliente precisa saber quando voltar")
                        .isNotNull());
    }

    @Test
    @DisplayName("o preflight do navegador nao consome cota")
    void preflightNaoConsomeCota() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(options("/v1/auth/login")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST"));
        }

        // Navegar normalmente dispara vários preflights. Se eles consumissem
        // cota, o usuário seria bloqueado sem ter tentado autenticar uma vez.
        mockMvc.perform(login("depois-dos-preflights@exemplo.test"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("uma tentativa real depois de 20 preflights ainda deve passar")
                        .isNotEqualTo(429));
    }

    @Test
    @DisplayName("requisicoes simultaneas nao furam o limite")
    void requisicoesSimultaneasNaoFuramOLimite() throws Exception {
        // Um ataque não faz uma tentativa por vez. Se a contagem não for atômica,
        // várias requisições leem o mesmo valor e passam todas.
        RateLimiter limiter = new RateLimiter(5, Duration.ofSeconds(60));
        int threads = 50;
        AtomicInteger permitidas = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch terminaram = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        // Todas esperam a mesma largada: sem isso as threads
                        // rodariam em sequência e a corrida não aconteceria.
                        largada.await();
                        if (limiter.tryAcquire("mesma-origem")) {
                            permitidas.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        terminaram.countDown();
                    }
                });
            }

            largada.countDown();
            assertThat(terminaram.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(permitidas.get())
                .as("com 50 tentativas simultâneas e limite 5, exatamente 5 podem passar")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("janelas expiradas sao descartadas da memoria")
    void janelasExpiradasSaoDescartadas() {
        // Sem limpeza, cada endereço que já tentou deixaria uma entrada
        // permanente, e a proteção viraria um jeito de esgotar a memória.
        //
        // O tempo avança pelo relógio, não pelo cronômetro da máquina: a versão
        // anterior usava janela de 1 ms e dependia de o relógio virar entre as
        // duas linhas, o que fazia o teste passar ou falhar conforme a carga.
        Instant inicio = Instant.parse("2026-01-01T10:00:00Z");
        MutableClock clock = new MutableClock(inicio);
        RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1), clock);

        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire("origem-" + i);
        }
        assertThat(limiter.trackedKeys()).isEqualTo(100);

        clock.advance(Duration.ofMinutes(2));
        limiter.evictExpired();

        assertThat(limiter.trackedKeys())
                .as("janelas vencidas não podem ficar acumuladas")
                .isZero();
    }

    @Test
    @DisplayName("a cota volta quando a janela expira")
    void cotaVoltaQuandoAJanelaExpira() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire("origem")).isTrue();
        assertThat(limiter.tryAcquire("origem")).isTrue();
        assertThat(limiter.tryAcquire("origem"))
                .as("a terceira tentativa dentro da janela é recusada")
                .isFalse();

        clock.advance(Duration.ofMinutes(2));

        assertThat(limiter.tryAcquire("origem"))
                .as("passada a janela, quem foi bloqueado precisa conseguir tentar de novo")
                .isTrue();
    }

    /** Relógio que só anda quando o teste manda. */
    private static final class MutableClock extends Clock {
        private Instant agora;

        private MutableClock(Instant inicio) {
            this.agora = inicio;
        }

        void advance(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String email) {
        return post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"senha-qualquer"}
                        """.formatted(email));
    }
}
