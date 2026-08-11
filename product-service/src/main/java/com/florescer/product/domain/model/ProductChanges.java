package com.florescer.product.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

	/**
	 * Os nomes dos campos que vieram preenchidos.
	 *
	 * <p>Serve ao registro de auditoria: saber que um produto foi alterado sem
	 * saber o quê não responde nada. Só os nomes, nunca os valores, porque o
	 * log tem retenção mais longa e acesso mais amplo que o banco.
	 */
	public List<String> camposPreenchidos() {
		List<String> campos = new ArrayList<>();
		if (name != null) campos.add("name");
		if (type != null) campos.add("type");
		if (description != null) campos.add("description");
		if (price != null) campos.add("price");
		if (quantityStock != null) campos.add("quantityStock");
		if (careRequirements != null) campos.add("careRequirements");
		if (availability != null) campos.add("availability");
		if (status != null) campos.add("status");
		return campos;
	}

	public boolean isEmpty() {
		return Stream.of(name, type, description, price, quantityStock, careRequirements, availability, status)
				.allMatch(Objects::isNull);
	}
}
