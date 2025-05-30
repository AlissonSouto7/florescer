package com.florescer.auth.infra.persistence;

import org.springframework.stereotype.Repository;

import com.florescer.auth.application.repository.RoleRepository;
import com.florescer.auth.domain.entity.Role;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
	
	private final RoleJPARepository jpaRepository;
	
	@Override
	public Role findByName(String name) {
		return jpaRepository.findByName(name);
	}
}