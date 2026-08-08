package com.florescer.auth.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.florescer.auth.application.repository.RoleRepository;
import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.entity.Role;
import com.florescer.auth.domain.entity.User;

import lombok.extern.log4j.Log4j2;

/**
 * Cria a conta administrativa inicial.
 *
 * <p>Desligado por padrão. Só roda quando {@code app.admin.enabled} é verdadeiro,
 * e nesse caso o e-mail e a senha são obrigatórios: um valor padrão significaria
 * uma conta com senha conhecida em qualquer ambiente onde alguém esquecesse de
 * configurar, que foi exatamente o problema que este código tinha.
 *
 * <p>A senha nunca é registrada em log.
 */
@Configuration
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true")
@Log4j2
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email}") String adminEmail,
                                @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "app.admin.enabled esta ligado, entao app.admin.email e app.admin.password sao obrigatorios.");
            }

            if (userRepository.findByEmail(adminEmail).isPresent()) {
                log.info("Conta administrativa inicial ja existe, nada a fazer.");
                return;
            }

            Role adminRole = roleRepository.findByName(Role.Values.ADMIN.name());

            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);
            log.info("Conta administrativa inicial criada.");
        };
    }
}
