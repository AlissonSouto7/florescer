package com.florescer.product.infra.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.model.ProductFilter;

import jakarta.persistence.criteria.Predicate;

/**
 * Monta a consulta da vitrine a partir dos filtros informados.
 *
 * <p>Usa {@code Specification} em vez de um método por combinação: com seis
 * filtros opcionais seriam dezenas de assinaturas em repositório, e cada filtro
 * novo dobraria o número. Aqui cada um é uma condição independente, e as
 * presentes são combinadas com E.
 *
 * <p>Nada de concatenar SQL: as condições viram parâmetros vinculados, então
 * valor vindo do cliente não altera a estrutura da consulta.
 */
public final class ProductSpecification {

	private ProductSpecification() {
	}

	public static Specification<Product> from(ProductFilter filtro) {
		return (root, query, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();

			if (filtro.light() != null) {
				condicoes.add(cb.equal(root.get("light"), filtro.light()));
			}
			if (filtro.petSafe() != null) {
				condicoes.add(cb.equal(root.get("petSafe"), filtro.petSafe()));
			}
			if (filtro.environment() != null) {
				// AMBOS atende quem procura interno e quem procura externo: uma
				// planta que vai bem nos dois lugares não pode sumir de nenhuma
				// das duas buscas.
				condicoes.add(cb.or(
						cb.equal(root.get("environment"), filtro.environment()),
						cb.equal(root.get("environment"), com.florescer.product.domain.enums.Environment.AMBOS)));
			}
			if (filtro.difficulty() != null) {
				condicoes.add(cb.equal(root.get("difficulty"), filtro.difficulty()));
			}
			if (filtro.maxPrice() != null) {
				condicoes.add(cb.lessThanOrEqualTo(root.get("price"), filtro.maxPrice()));
			}
			if (Boolean.TRUE.equals(filtro.onlyAvailable())) {
				// Duas condições, porque são coisas diferentes: a planta pode
				// estar marcada como disponível e ter acabado o estoque.
				condicoes.add(cb.isTrue(root.get("availability")));
				condicoes.add(cb.greaterThan(root.get("quantityStock"), 0));
			}

			return condicoes.isEmpty() ? cb.conjunction() : cb.and(condicoes.toArray(new Predicate[0]));
		};
	}
}
