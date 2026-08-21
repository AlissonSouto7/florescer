package com.florescer.product.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.florescer.product.domain.entity.ShopSettings;

/**
 * Acesso à única linha de configuração da loja.
 *
 * <p>Não há método de busca por critério, e não deveria haver: a linha é sempre
 * a de identificador {@link ShopSettings#ID_UNICO}, garantido por CHECK no
 * banco. Um {@code findAll} aqui sugeriria que pode existir mais de uma.
 */
public interface ShopSettingsJPARepository extends JpaRepository<ShopSettings, Integer> {
}
