package com.florescer.auth.infrastructure.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Dá a cada requisição um identificador que atravessa os serviços.
 *
 * <p>Sem isso, uma requisição que passa pelo auth e depois pelo product produz
 * linhas de log nos dois lados sem nada que as ligue, e investigar um erro
 * relatado por um cliente vira busca por horário aproximado.
 *
 * <p>O identificador é aceito do cliente quando já vem no cabeçalho, para que a
 * cadeia se mantenha entre chamadas, e gerado quando não vem. Como o valor entra
 * no log e volta na resposta, ele é <b>validado</b> antes de ser usado: um valor
 * de fora com quebra de linha permitiria forjar uma linha de log inteira, que é
 * a mesma falha que o CodeQL apontou no filtro de rate limit.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Correlation-Id";
	public static final String MDC_KEY = "correlationId";

	/** Formato aceito de fora: só o que um identificador precisa ser. */
	private static final Pattern ACEITO = Pattern.compile("[A-Za-z0-9-]{8,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String correlationId = resolve(request.getHeader(HEADER));

		MDC.put(MDC_KEY, correlationId);
		// Devolvido sempre, inclusive em erro: é o número que a pessoa informa ao
		// pedir ajuda, e sem ele o suporte não tem por onde começar.
		response.setHeader(HEADER, correlationId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			// O MDC vive na thread, e a thread volta para o pool. Sem esta
			// limpeza, a próxima requisição atendida por ela herdaria o
			// identificador da anterior.
			MDC.remove(MDC_KEY);
		}
	}

	private String resolve(String recebido) {
		if (recebido != null && ACEITO.matcher(recebido).matches()) {
			return recebido;
		}
		return UUID.randomUUID().toString();
	}
}
