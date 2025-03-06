package com.florescer.auth.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.florescer.auth.domain.entity.Role;

@Repository
public interface RoleJPARepository extends JpaRepository<Role, Long> {

	Role findByName(String name);
	
}