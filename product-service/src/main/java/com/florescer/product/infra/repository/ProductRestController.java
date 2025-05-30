package com.florescer.product.infra.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.florescer.product.domain.entity.Product;
import com.florescer.product.domain.exception.personalizadas.DatabaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Repository
@RequiredArgsConstructor
@Log4j2	
public class ProductRestController implements ProductRepository {

	private final ProductJPARepository repository;
	
	@Override
	public Product save(Product product) {
		try {
	        return repository.save(product);
	    } catch (DataAccessException e) {
	        log.error("Erro ao salvar produto no banco", e);
	        throw new DatabaseException("Erro ao salvar produto", e);
	    }
	}

	@Override
	public Page<Product> findAll(Pageable pageable) {
		return repository.findAll(pageable);
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