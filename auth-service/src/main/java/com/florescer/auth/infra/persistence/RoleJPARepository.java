package com.florescer.auth.infra.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.florescer.auth.domain.entity.Role;

@Repository
public interface RoleJPARepository extends JpaRepository<Role, Long> {
	Optional<Role> findByName(String name);	
}