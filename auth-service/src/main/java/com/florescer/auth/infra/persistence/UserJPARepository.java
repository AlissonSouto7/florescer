package com.florescer.auth.infra.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.florescer.auth.domain.entity.User;

@Repository
public interface UserJPARepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);
}