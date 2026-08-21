package com.florescer.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.auth.support.AbstractIntegrationTest;
import com.florescer.auth.support.TestTokens;

/**
 * Garante que a autenticação por token continua valendo.
 *
 * <p>Escrito antes de remover o {@code JwtAuthFilter}, para que a remoção seja
 * verificada e não apenas argumentada: se o filtro fosse necessário para
 * proteger alguma coisa, estes casos passariam a falhar.
 *
 * <p>O serviço não expõe endpoint protegido próprio hoje, então a verificação
 * usa uma rota inexistente. A regra {@code anyRequest().authenticated()} vale
 * para ela, e é justamente a resposta a quem não se identificou que interessa.
 */
@AutoConfigureMockMvc
class TokenAuthenticationTest extends AbstractIntegrationTest {

    private static final String ROTA_PROTEGIDA = "/v1/qualquer-rota-protegida";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("rota protegida sem token responde 401")
    void semTokenResponde401() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token assinado por outra chave e recusado")
    void tokenDeOutraChaveERecusado() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA)
                        .header("Authorization", "Bearer " + TestTokens.deOutraChave("alguem@exemplo.test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token malformado e recusado sem quebrar o servidor")
    void tokenMalformadoERecusado() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA)
                        .header("Authorization", "Bearer nao.e.um.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token expirado e recusado")
    void tokenExpiradoERecusado() throws Exception {
        mockMvc.perform(get(ROTA_PROTEGIDA)
                        .header("Authorization", "Bearer " + TestTokens.expirado("alguem@exemplo.test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token valido passa da autenticacao e nao para em 401")
    void tokenValidoEAceito() throws Exception {
        // A rota não existe, então o esperado é 404. O que este caso protege é a
        // diferença entre 404 e 401: chegar ao 404 prova que a autenticação
        // aceitou o token, e não que ele foi barrado antes de rotear.
        mockMvc.perform(get(ROTA_PROTEGIDA)
                        .header("Authorization", "Bearer " + TestTokens.valido("alguem@exemplo.test")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("as rotas publicas continuam publicas")
    void rotasPublicasContinuamPublicas() throws Exception {
        // Login com credencial errada responde 401 do fluxo de autenticação, e
        // não 401 por falta de token: o importante é que a rota é alcançável.
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alguem@exemplo.test","password":"senha que nao confere"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
