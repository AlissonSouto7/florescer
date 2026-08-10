package com.florescer.auth.infrastructure.ratelimit;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

/**
 * Recusa tentativas em excesso nos endpoints de autenticação.
 *
 * <p>Sem limite, descobrir uma senha depende só de tempo de máquina: o atacante
 * tenta indefinidamente e o servidor responde a todas. É particularmente sério
 * aqui porque existe uma conta administrativa cujo endereço é previsível.
 *
 * <p>Roda antes da autenticação, senão cada tentativa recusada ainda custaria
 * uma consulta ao banco e uma verificação de BCrypt, que é cara de propósito.
 * Nesse cenário o próprio mecanismo de defesa vira o ponto de esgotamento.
 */
@Log4j2
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/v1/auth/login", "/v1/auth/register");

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getRequestURI e não getServletPath: o segundo depende de como a
        // aplicação está mapeada no contêiner e chega vazio quando ela responde
        // na raiz, fazendo a comparação nunca casar e o filtro se excluir de
        // tudo em silêncio.
        return !PROTECTED_PATHS.contains(request.getRequestURI())
                // O preflight do navegador não é tentativa de autenticação e não
                // pode consumir cota, senão navegar esgotaria o limite sozinho.
                || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clientKey = clientKey(request);

        if (rateLimiter.tryAcquire(clientKey)) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = rateLimiter.secondsUntilReset(clientKey);

        // O caminho registrado vem da constante, e não da requisição. Escrever
        // direto o valor recebido permitiria injetar quebras de linha e forjar
        // entradas no log, corrompendo a mesma auditoria que este filtro
        // alimenta. O log também não traz endereço nem corpo: registra que
        // houve excesso, não quem tentou o quê.
        String path = matchedPath(request);
        log.warn("Limite de tentativas excedido em {} ({}s para liberar)", path, retryAfter);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.getWriter().write("""
                {"error":"Muitas tentativas","details":"Aguarde alguns instantes e tente novamente."}
                """);
    }

    /**
     * Devolve o caminho protegido correspondente, tirado da própria constante.
     *
     * <p>Parece equivalente a usar o valor da requisição, já que o filtro só roda
     * quando os dois coincidem, e não é: devolver a instância da constante
     * garante que nada vindo de fora chega ao log, independentemente do que a
     * comparação aceite hoje ou depois.
     */
    private String matchedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return PROTECTED_PATHS.stream()
                .filter(uri::equals)
                .findFirst()
                .orElse("rota protegida");
    }

    /**
     * Identifica o cliente pelo endereço de origem.
     *
     * <p>Atrás de proxy ou balanceador, o endereço da conexão é o do próprio
     * proxy, e todos os usuários compartilhariam a mesma cota. O
     * {@code X-Forwarded-For} resolve isso, mas só é confiável quando o proxy é
     * quem o preenche: exposto direto na internet, o cliente escolhe o valor e
     * escapa do limite trocando de valor a cada tentativa.
     */
    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
