package com.florescer.product.domain.model;

import java.math.BigDecimal;

import com.florescer.product.domain.enums.Difficulty;
import com.florescer.product.domain.enums.Environment;
import com.florescer.product.domain.enums.Light;

/**
 * Filtros da vitrine. Campo nulo significa "não filtre por isso".
 *
 * <p>Existem porque a lista solta obriga quem visita a abrir planta por planta
 * para descobrir qual serve. Quem tem gato precisa ver só as seguras, quem mora
 * em apartamento sem sol precisa ver só as de sombra, e quem tem um teto de
 * preço não quer se apaixonar pelo que não vai comprar.
 *
 * <p>O filtro roda no banco, e não no navegador: mandar o catálogo inteiro para
 * o cliente filtrar desperdiça banda de quem está no celular e piora conforme o
 * catálogo cresce.
 */
public record ProductFilter(
		Light light,
		Boolean petSafe,
		Environment environment,
		Difficulty difficulty,
		BigDecimal maxPrice,
		Boolean onlyAvailable) {

	/** Nenhum filtro: a vitrine inteira. */
	public static ProductFilter nenhum() {
		return new ProductFilter(null, null, null, null, null, null);
	}

	public boolean isEmpty() {
		return light == null && petSafe == null && environment == null
				&& difficulty == null && maxPrice == null && onlyAvailable == null;
	}
}
