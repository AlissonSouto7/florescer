package com.florescer.product.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.florescer.product.domain.entity.Product;

public interface ProductRepository {

	public Product save(Product product);

	public List<Product> getListProduct();

	public Optional<Product> findById(UUID productId);

	public void delete(Product product);

}