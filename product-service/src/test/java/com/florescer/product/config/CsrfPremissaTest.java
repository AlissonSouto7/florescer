package com.florescer.product.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;

/**
 * A premissa que torna seguro desligar a proteção de CSRF.
 *
 * <p>O CodeQL marca {@code csrf.disable()} como falha de severidade alta, e a
 * regra está certa na maioria dos sistemas: ela assume sessão por cookie. Aqui
 * ela é falso positivo, mas <b>só enquanto a premissa valer</b>.
 *
 * <p>CSRF existe porque o navegador anexa credencial sozinho em requisição
 * cross-site. É isso que permite ao site do atacante disparar uma escrita
 * "assinada" pela vítima sem nunca ler nada dela. <b>Sem credencial ambiente,
 * não há ataque</b>: quem invoca precisa escrever o token no cabeçalho, e para
 * isso precisaria lê-lo, o que a origem dele impede.
 *
 * <p>Este serviço é o que tem as rotas de escrita que importam: cadastrar,
 * editar e excluir planta, e trocar o WhatsApp que recebe os pedidos.
 *
 * <p>O teste existe para o dia em que alguém adicionar autenticação por cookie.
 * Nesse dia a premissa cai, o CSRF volta a se aplicar, e é aqui que isso
 * aparece, em vez de aparecer num catálogo alterado por terceiro.
 */
@AutoConfigureMockMvc
class CsrfPremissaTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("nenhuma sessão de servidor é criada, nem na leitura pública")
    void nenhumaSessaoEhCriada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v1/product")).andReturn();

        // A verificação olha a sessão, e não o Set-Cookie da resposta. O
        // MockMvc não emite o cabeçalho como um container de verdade, então
        // verificar o cabeçalho passa mesmo com a sessão ligada: foi o que
        // aconteceu na primeira versão deste teste, e a mutação que trocava
        // STATELESS por ALWAYS escapou.
        assertThat(resultado.getRequest().getSession(false))
                .as("sessão criada significa JSESSIONID, que é credencial ambiente e traz o CSRF de volta")
                .isNull();

        assertThat(resultado.getResponse().getCookies())
                .as("cookie devolvido aqui viraria credencial ambiente nas escritas")
                .isEmpty();
    }

    @Test
    @DisplayName("excluir só com cookie, sem Authorization, é recusado")
    void excluirComCookieEhRecusado() throws Exception {
        // Este é o ataque, na forma mais direta: o navegador da vendedora
        // mandando um DELETE a partir do site do atacante, levando o que
        // levaria sozinho. Sem credencial ambiente, chega como anônimo.
        int status = mockMvc.perform(delete("/v1/product/{id}", "00000000-0000-0000-0000-000000000000")
                .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "forjado")))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("o cookie não pode autenticar: se autenticar, o CSRF volta a valer")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("trocar os dados da loja só com cookie é recusado")
    void trocarDadosDaLojaComCookieEhRecusado() throws Exception {
        // O alvo mais valioso do sistema: quem troca este número redireciona
        // todos os pedidos.
        int status = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/v1/settings")
                .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "forjado"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"whatsappNumber\":\"5511999999999\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }
}
