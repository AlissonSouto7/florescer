package com.florescer.auth.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.florescer.auth.domain.Role;

@Repository
public interface RoleJPARepository extends JpaRepository<Role, Long> {
	
}