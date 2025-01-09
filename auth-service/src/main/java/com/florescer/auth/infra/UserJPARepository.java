package com.florescer.auth.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.florescer.auth.application.domain.User;

@Repository
public interface UserJPARepository extends JpaRepository<User, UUID> {

}
