package com.florescer.product.domain.model;

import java.math.BigDecimal;

import com.florescer.product.domain.enums.Status;

/**
 * Dados para cadastrar um produto.
 *
 * <p>Existe para o domínio não depender do DTO da API. São parecidos hoje e não
 * são a mesma coisa: o DTO carrega as regras de validação da borda e as
 * anotações de documentação, e muda quando o contrato HTTP muda. Este comando
 * muda quando a regra de negócio muda.
 */
public record NewProduct(
		String name,
		String type,
		String description,
		BigDecimal price,
		Integer quantityStock,
		String careRequirements,
		Boolean availability,
		Status status) {
}
