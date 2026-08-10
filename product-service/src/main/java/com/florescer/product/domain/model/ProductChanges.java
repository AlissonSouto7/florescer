package com.florescer.product.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

import com.florescer.product.domain.enums.Status;

/**
 * Alterações parciais de um produto: campo nulo significa "não mexa".
 *
 * <p>O {@link #isEmpty()} vive aqui, e não no serviço, porque saber se um
 * conjunto de mudanças está vazio é característica do próprio conjunto.
 */
public record ProductChanges(
		String name,
		String type,
		String description,
		BigDecimal price,
		Integer quantityStock,
		String careRequirements,
		Boolean availability,
		Status status) {

	public boolean isEmpty() {
		return Stream.of(name, type, description, price, quantityStock, careRequirements, availability, status)
				.allMatch(Objects::isNull);
	}
}
