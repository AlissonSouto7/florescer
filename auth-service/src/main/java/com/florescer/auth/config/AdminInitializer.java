package com.florescer.auth.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.entity.Role;
import com.florescer.auth.domain.entity.User;
import com.florescer.auth.infra.persistence.RoleJPARepository;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, RoleJPARepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@florescer.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                Role adminRole = roleRepository.findByName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrada"));
                
                User admin = new User();
                admin.setName("Administrador");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(adminRole));

                userRepository.save(admin);
                System.out.println("Admin criado com sucesso: admin@florescer.com / admin123");
            }
        };
    }
}
