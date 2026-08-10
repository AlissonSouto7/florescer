package com.florescer.auth.exception.custom;

/**
 * O e-mail informado já pertence a uma conta.
 *
 * <p>A mensagem não carrega o endereço de propósito: ela era usada tanto no log
 * quanto no corpo da resposta, então o valor acabava gravado e devolvido ao
 * cliente. Quem precisa identificar a tentativa no log usa o pseudônimo.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailAlreadyRegisteredException() {
		super("E-mail já registrado.");
	}
}
