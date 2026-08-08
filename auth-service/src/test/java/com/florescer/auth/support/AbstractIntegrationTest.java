package com.florescer.auth.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes que sobem o contexto Spring.
 *
 * <p>O banco é um MySQL real em container, o mesmo motor da produção: SQL específico,
 * collation e tipos se comportam de forma diferente em bancos em memória.
 *
 * <p>O container é estático e compartilhado por toda a suíte, então sobe uma única vez.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("authdb");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("jwt.private-key", RsaTestKeys::privateKeyPem);
        registry.add("jwt.public-key", RsaTestKeys::publicKeyPem);
    }
}
