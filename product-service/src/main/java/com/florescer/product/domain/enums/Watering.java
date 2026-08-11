package com.florescer.product.domain.enums;

/**
 * De quanto em quanto tempo regar.
 *
 * <p>Frequência em vez de texto livre porque é a diferença entre um compromisso
 * diário e um mensal, e porque assim dá para filtrar por quem viaja muito.
 */
public enum Watering {

	DIARIA,
	DUAS_A_TRES_VEZES_SEMANA,
	SEMANAL,
	QUINZENAL,
	MENSAL
}
