package com.florescer.product.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.infra.ProductJPARepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

	private final ProductJPARepository repository;
	
	@Override
	public Product save(Product product) {
		return repository.save(product);
	}

	@Override
	public List<Product> getListProduct() {
		return repository.findAll();
	}

	@Override
	public Optional<Product> findById(UUID productId) {
		return repository.findById(productId);
	}

	@Override
	public void delete(Product product) {
		repository.delete(product);
	}
}