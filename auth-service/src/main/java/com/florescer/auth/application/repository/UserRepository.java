package com.florescer.auth.application.repository;

import java.util.Optional;

import com.florescer.auth.domain.entity.User;

public interface UserRepository {

	Optional<User> findByEmail(String email);

	User save(User user);

}
