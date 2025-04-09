package com.florescer.product.infra.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.florescer.product.domain.entity.Product;

public interface ProductJPARepository extends JpaRepository<Product, UUID>{
}