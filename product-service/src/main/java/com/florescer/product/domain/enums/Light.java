package com.florescer.product.domain.enums;

/**
 * Quanta luz a planta aguenta.
 *
 * <p>É o campo que mais decide se ela sobrevive depois da venda: quem coloca uma
 * planta de sombra na janela sul mata a planta e não volta a comprar.
 */
public enum Light {

	/** Aguenta sol direto boa parte do dia. */
	SOL_PLENO,

	/** Claridade sim, sol direto não, ou só o do começo da manhã. */
	MEIA_SOMBRA,

	/** Vive longe da janela, com luz indireta. */
	SOMBRA
}
