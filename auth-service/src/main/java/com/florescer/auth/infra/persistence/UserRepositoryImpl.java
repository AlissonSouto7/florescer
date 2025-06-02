package com.florescer.auth.infra.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
	
	private final UserJPARepository jpaRepository;
	
	@Override
	public Optional<User> findByEmail(String email) {
		Optional<User> user = jpaRepository.findByEmail(email);
		return user;
	}

	@Override
	public User save(User user) {
		return jpaRepository.save(user);
	}

}