package com.florescer.product.infra.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.florescer.product.domain.entity.Product;

public interface ProductRepository {
	public Product save(Product product);

	public Page<Product> findAll(Pageable pageable);

	public Optional<Product> findById(UUID productId);

	public void delete(Product product);
}