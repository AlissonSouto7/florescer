package com.florescer.auth.domain;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * A política de senha vale no cadastro, e não no login.
 *
 * <p>No login a senha ou confere ou não confere. Recusar por formato entrega a
 * política a quem está tentando adivinhar, e tranca fora quem cadastrou antes de
 * a regra mudar, sem ter feito nada.
 */
@AutoConfigureMockMvc
class PasswordPolicyTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("cadastro aceita passphrase longa")
    void cadastroAceitaPassphraseLonga() throws Exception {
        // O caso que o teto de 20 impedia: frase longa, fácil de lembrar e
        // custosa de quebrar.
        mockMvc.perform(registrar("passphrase@exemplo.test", "cavalo bateria grampo correto"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("cadastro recusa senha curta")
    void cadastroRecusaSenhaCurta() throws Exception {
        mockMvc.perform(registrar("curta@exemplo.test", "S3nh@!"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cadastro recusa senha acima do limite do algoritmo")
    void cadastroRecusaSenhaAcimaDoLimite() throws Exception {
        // Acima de 72 bytes o BCrypt ignora o excedente. Recusar é melhor que
        // truncar em silêncio e deixar o usuário achar que está mais protegido.
        mockMvc.perform(registrar("gigante@exemplo.test", "a".repeat(100)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login com senha curta responde 401, nao 400")
    void loginComSenhaCurtaResponde401() throws Exception {
        // 400 diria "sua senha não tem o formato certo", o que informa a regra a
        // quem está testando combinações. A resposta correta é credencial
        // inválida, igual a qualquer outra tentativa errada.
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qualquer@exemplo.test","password":"abc"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login sem senha continua sendo requisicao invalida")
    void loginSemSenhaEhRequisicaoInvalida() throws Exception {
        // Campo ausente é erro de formato da requisição, não tentativa de
        // autenticação: aqui 400 é o correto.
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"qualquer@exemplo.test","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registrar(
            String email, String senha) {
        return post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Pessoa","email":"%s","password":"%s"}
                        """.formatted(email, senha));
    }
}
