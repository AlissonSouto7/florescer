package com.florescer.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.application.repository.UserRepository;
import com.florescer.auth.domain.entity.User;
import com.florescer.auth.infrastructure.persistence.UserJPARepository;
import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * Duas pessoas registrando o mesmo e-mail ao mesmo tempo.
 *
 * <p>A verificação de disponibilidade é um {@code findByEmail} seguido de um
 * insert, e entre os dois existe uma janela. Duas requisições que passem juntas
 * pela consulta chegam juntas ao insert, e o e-mail deixa de identificar uma
 * conta só: o login passa a depender de qual das duas o banco devolve primeiro.
 *
 * <p>A corrida é forçada, não esperada. Um teste que dispare duas threads e torça
 * pela sobreposição passa por sorte e falha por sorte, e {@code sleep} não é
 * prova de nada. Aqui a segunda requisição é disparada de dentro da consulta da
 * primeira, então a ordem é sempre a mesma: a primeira olha o banco, a segunda
 * completa o registro inteiro, e só então a primeira tenta gravar.
 */
@AutoConfigureMockMvc
class ConcurrentRegistrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "corrida@exemplo.test";
    private static final String SENHA = "uma senha bem comprida";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJPARepository userJPARepository;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Test
    @DisplayName("registro simultaneo do mesmo e-mail cria uma conta so")
    void registroSimultaneoCriaUmaContaSo() throws Exception {
        AtomicBoolean primeiraConsulta = new AtomicBoolean(true);
        CompletableFuture<Integer> statusDoConcorrente = new CompletableFuture<>();

        doAnswer(invocacao -> {
            @SuppressWarnings("unchecked")
            Optional<User> resultado = (Optional<User>) invocacao.callRealMethod();

            // Só na primeira passagem, senão a requisição concorrente entraria
            // aqui de novo e o teste viraria uma recursão.
            if (primeiraConsulta.compareAndSet(true, false)) {
                // Em outra thread, com transação própria: o registro concorrente
                // precisa estar confirmado no banco antes de a primeira
                // requisição seguir para o insert.
                statusDoConcorrente.complete(
                        CompletableFuture.supplyAsync(this::registrarIgnorandoErro).join());
            }
            return resultado;
        }).when(userRepository).findByEmail(eq(EMAIL));

        int statusDaPrimeira = registrar();

        assertThat(statusDoConcorrente.join())
                .as("a requisição que chegou ao banco primeiro deve criar a conta")
                .isEqualTo(201);

        assertThat(statusDaPrimeira)
                .as("a segunda deve receber o mesmo 409 do caminho normal, e não um erro de servidor")
                .isEqualTo(409);

        assertThat(userJPARepository.findAll().stream()
                .filter(u -> EMAIL.equals(u.getEmail()))
                .count())
                .as("o e-mail precisa continuar identificando uma conta só")
                .isEqualTo(1);
    }

    private int registrar() throws Exception {
        return mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pessoa","email":"%s","password":"%s"}
                                """.formatted(EMAIL, SENHA)))
                .andReturn().getResponse().getStatus();
    }

    private int registrarIgnorandoErro() {
        try {
            return registrar();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao registrar em paralelo", ex);
        }
    }
}
