package com.florescer.auth.application.repository;

import com.florescer.auth.domain.entity.Role;

public interface RoleRepository {
	Role findByName(String name);
}