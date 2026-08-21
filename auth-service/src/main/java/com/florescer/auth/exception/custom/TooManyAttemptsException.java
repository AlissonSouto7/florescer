package com.florescer.auth.exception.custom;

import lombok.Getter;

/**
 * Erros de senha demais para esta conta na janela atual.
 *
 * <p>Só chega aqui quem <b>errou</b> a senha: quem acerta entra mesmo com o
 * contador estourado, senão errar a senha de alguém viraria uma forma de
 * trancar essa pessoa fora do painel.
 *
 * <p>A resposta é a mesma do limite por origem, `429`, e a mensagem não
 * distingue "esta conta existe e está bloqueada" de "você tentou demais": dizer
 * o contrário entregaria, de graça, quais endereços têm conta na loja.
 */
@Getter
public class TooManyAttemptsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Quanto falta para a janela virar, para o cabeçalho {@code Retry-After}. */
    private final long secondsUntilReset;

    public TooManyAttemptsException(long secondsUntilReset) {
        super("Muitas tentativas.");
        this.secondsUntilReset = secondsUntilReset;
    }
}
