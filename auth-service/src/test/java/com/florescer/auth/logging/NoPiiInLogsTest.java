package com.florescer.auth.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;
import com.florescer.auth.support.LogCapture;

/**
 * O log não pode virar uma segunda base de dados pessoais.
 *
 * <p>A verificação olha o que a aplicação registrou, e não a saída do processo:
 * o ferramental de teste também escreve no stdout, e o MockMvc chega a imprimir
 * o corpo da requisição quando uma asserção falha. Ler o stdout faria um teste
 * acusar o relatório de falha de outro.
 */
@AutoConfigureMockMvc
class NoPiiInLogsTest extends AbstractIntegrationTest {

    private static final String EMAIL = "pessoa.identificavel@exemplo.test";
    private static final String SENHA = "uma-senha-secreta";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("falha de login nao grava o e-mail tentado")
    void falhaDeLoginNaoGravaEmail() throws Exception {
        try (LogCapture log = LogCapture.start()) {
            tentarLogin();

            assertThat(log.all())
                    .as("o endereço tentado não pode aparecer no log")
                    .doesNotContain(EMAIL);
        }
    }

    @Test
    @DisplayName("nenhuma senha aparece no log")
    void senhaNuncaAparece() throws Exception {
        try (LogCapture log = LogCapture.start()) {
            tentarLogin();
            registrar();

            assertThat(log.all())
                    .as("senha não pode aparecer em log em nenhuma hipótese")
                    .doesNotContain(SENHA);
        }
    }

    @Test
    @DisplayName("registro duplicado nao grava nem devolve o e-mail")
    void registroDuplicadoNaoExpoeEmail() throws Exception {
        registrar();

        try (LogCapture log = LogCapture.start()) {
            String resposta = registrar();

            assertThat(log.all())
                    .as("o endereço não pode aparecer no log do registro duplicado")
                    .doesNotContain(EMAIL);
            assertThat(resposta)
                    .as("a resposta de conflito não pode devolver o endereço informado")
                    .doesNotContain(EMAIL);
        }
    }

    @Test
    @DisplayName("a tentativa continua auditavel por um identificador estavel")
    void tentativaContinuaAuditavel() throws Exception {
        try (LogCapture log = LogCapture.start()) {
            tentarLogin();

            // Sem isto, "não vazar PII" seria satisfeito apagando o log inteiro,
            // e a auditoria de tentativas de autenticação se perderia junto.
            assertThat(log.all())
                    .as("a tentativa precisa continuar registrada, com identificador no lugar do e-mail")
                    .contains("subject=");
        }
    }

    private void tentarLogin() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, SENHA)));
    }

    private String registrar() throws Exception {
        return mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pessoa","email":"%s","password":"%s"}
                                """.formatted(EMAIL, SENHA)))
                .andReturn().getResponse().getContentAsString();
    }
}
