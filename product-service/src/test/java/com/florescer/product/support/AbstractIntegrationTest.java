package com.florescer.product.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes que sobem o contexto Spring.
 *
 * <p>O banco é um PostgreSQL real em container, o mesmo motor da produção: SQL específico,
 * tipos e comportamento de transação diferem em bancos em memória.
 *
 * <p>O container é estático e compartilhado por toda a suíte, então sobe uma única vez.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("dbproduct");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.public-key", RsaTestKeys::publicKeyPem);
    }
}
