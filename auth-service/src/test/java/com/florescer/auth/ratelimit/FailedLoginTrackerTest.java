package com.florescer.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.florescer.auth.infrastructure.ratelimit.InMemoryFailedLoginTracker;
import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * O que estes testes protegem.
 *
 * <p>Limitar login por endereço não funciona neste sistema, e as duas formas de
 * tentar falham de lados opostos. Ambas foram medidas antes de existir este
 * código:
 *
 * <ul>
 *   <li><b>pelo endereço da conexão</b>: em produção o navegador fala com o
 *       site, e o site repassa, então toda tentativa chega do mesmo lugar. Dez
 *       senhas erradas de um desconhecido, e a vendedora recebeu {@code 429} na
 *       senha <b>certa</b>;
 *   <li><b>pelo {@code X-Forwarded-For}</b>: o cliente manda esse cabeçalho.
 *       Quinze tentativas trocando o valor a cada uma, <b>nenhum 429</b>. O
 *       limite deixou de existir.
 * </ul>
 *
 * <p>A conta alvo o atacante não escolhe, e é por aí que a contagem passou a
 * ser feita. A regra que evita virar bloqueio: <b>a senha certa passa sempre</b>.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.login-attempts.max-failures=3",
        "app.login-attempts.window-seconds=900",
        // O limite por origem sai do caminho: aqui o que está sob teste é a
        // contagem por conta, e deixar os dois ativos esconderia qual dos dois
        // recusou.
        "app.rate-limit.max-attempts=1000"
})
class FailedLoginTrackerTest extends AbstractIntegrationTest {

    private static final String CONTA = "vendedora@florescer.com.br";
    private static final String SENHA = "senha-comprida-de-verdade-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryFailedLoginTracker contador;

    /** Cria a conta antes de tentar entrar nela. */
    @BeforeEach
    void criarConta() throws Exception {
        // Cada caso começa do zero: o contador vive em memória e sobrevive entre testes.
        contador.recordSuccess(CONTA);
        contador.recordSuccess("outra@florescer.com.br");
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Vendedora","email":"%s","password":"%s"}
                        """.formatted(CONTA, SENHA)));
    }

    private int tentar(String senha) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(CONTA, senha)))
                .andReturn();
        return resultado.getResponse().getStatus();
    }

    @Test
    @DisplayName("erros de senha na mesma conta acabam sendo recusados")
    void erroDemaisEhRecusado() throws Exception {
        assertThat(tentar("errada-1")).isEqualTo(401);
        assertThat(tentar("errada-2")).isEqualTo(401);
        assertThat(tentar("errada-3")).isEqualTo(401);

        // A partir daqui o contador estourou: continuar tentando não devolve
        // mais o 401 comum.
        assertThat(tentar("errada-4")).isEqualTo(429);
    }

    @Test
    @DisplayName("a senha certa entra mesmo com o contador estourado")
    void senhaCertaSemprePassa() throws Exception {
        // Este é o caso que o limite por origem quebrava: um estranho errando a
        // senha trancava a dona da conta.
        for (int i = 0; i < 6; i++) {
            tentar("chute-" + i);
        }

        assertThat(tentar(SENHA)).isEqualTo(200);
    }

    @Test
    @DisplayName("acertar zera a contagem, para o próximo esquecimento não bloquear")
    void acertarZera() throws Exception {
        tentar("errada-1");
        tentar("errada-2");
        assertThat(tentar(SENHA)).isEqualTo(200);

        // Sem o reset, a conta ficaria a um erro do bloqueio pelo resto da
        // janela, que aqui são 15 minutos.
        assertThat(tentar("errada-3")).isEqualTo(401);
        assertThat(tentar("errada-4")).isEqualTo(401);
    }

    @Test
    @DisplayName("a resposta não revela que a conta existe")
    void naoRevelaQueAContaExiste() throws Exception {
        for (int i = 0; i < 5; i++) {
            tentar("chute-" + i);
        }

        String corpo = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"outra"}
                        """.formatted(CONTA)))
                .andReturn().getResponse().getContentAsString();

        // "conta bloqueada" seria confirmação de que este endereço tem conta.
        assertThat(corpo).doesNotContain(CONTA);
        assertThat(corpo.toLowerCase()).doesNotContain("bloque");
        assertThat(corpo.toLowerCase()).doesNotContain("existe");
    }

    @Test
    @DisplayName("contas diferentes têm contagens separadas")
    void contasSeparadas() throws Exception {
        for (int i = 0; i < 5; i++) {
            tentar("chute-" + i);
        }

        // Outra pessoa errando a senha dela não pode ser afetada pelo ataque
        // contra esta conta.
        int status = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"outra@florescer.com.br","password":"qualquer"}
                        """))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }
}
