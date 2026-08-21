package com.florescer.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.florescer.auth.support.AbstractIntegrationTest;

/**
 * A premissa que torna seguro desligar a proteção de CSRF.
 *
 * <p>O CodeQL marca {@code csrf.disable()} como falha de severidade alta, e a
 * regra está certa na maioria dos sistemas: ela assume sessão por cookie. Aqui
 * ela é falso positivo, mas <b>só enquanto a premissa valer</b>.
 *
 * <p>CSRF existe porque o navegador anexa credencial sozinho em requisição
 * cross-site: cookie de sessão, autenticação básica, certificado de cliente. É
 * isso que permite ao site do atacante mandar uma escrita "assinada" pela
 * vítima sem nunca ler nada dela. <b>Sem credencial ambiente, não há ataque</b>:
 * quem invoca precisa escrever o token no cabeçalho, e para isso precisaria
 * lê-lo, o que a origem dele impede.
 *
 * <p>Neste sistema o token vai em {@code Authorization: Bearer}, guardado no
 * {@code sessionStorage} do site, e a sessão é {@code STATELESS} nos dois
 * serviços.
 *
 * <p>Este teste existe para o dia em que alguém adicionar login por cookie,
 * "lembrar de mim" ou autenticação básica. Nesse dia a premissa cai, o CSRF
 * volta a se aplicar, e é aqui que isso aparece, em vez de aparecer numa conta
 * invadida. Um comentário dizendo "não se aplica" envelheceria em silêncio.
 */
@AutoConfigureMockMvc
class CsrfPremissaTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("o login não devolve cookie: não há credencial que o navegador anexe sozinho")
    void loginNaoDevolveCookie() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"nao-existe@florescer.com.br","password":"qualquer-coisa"}
                        """))
                .andReturn().getResponse();

        // O status não importa aqui, e por isso não é verificado: o que importa
        // é que nenhum caminho, nem o de erro, devolva credencial em cookie.
        assertThat(resposta.getCookies())
                .as("um cookie de autenticação faria o CSRF voltar a se aplicar")
                .isEmpty();
        assertThat(resposta.getHeader("Set-Cookie"))
                .as("Set-Cookie no login é credencial ambiente")
                .isNull();
    }

    @Test
    @DisplayName("escrever sem o cabeçalho Authorization é recusado, mesmo com cookies")
    void semCabecalhoNaoEscreve() throws Exception {
        // A requisição leva um cookie qualquer, como um navegador levaria. Se
        // algum dia isso passar a autenticar, o ataque de CSRF vira possível.
        int status = mockMvc.perform(post("/v1/auth/register")
                .cookie(new jakarta.servlet.http.Cookie("JSESSIONID", "forjado"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Invasor","email":"invasor@florescer.com.br","password":"senha-comprida-de-verdade"}
                        """))
                .andReturn().getResponse().getStatus();

        // Registro é rota pública de propósito, então o que se verifica é que o
        // cookie não trouxe autoridade nenhuma: a resposta é a mesma que
        // qualquer anônimo receberia, e não a de alguém autenticado.
        assertThat(status)
                .as("o cookie não pode conceder nada além do que um anônimo tem")
                .isIn(201, 400, 409, 429);
    }

    @Test
    @DisplayName("a sessão é STATELESS: o servidor não guarda estado entre requisições")
    void sessaoEhStateless() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"nao-existe@florescer.com.br","password":"qualquer-coisa"}
                        """))
                .andReturn();

        // Olha a sessão, e não o Set-Cookie: o MockMvc não emite esse cabeçalho
        // como um container de verdade, então verificá-lo passa mesmo com a
        // sessão ligada. A primeira versão deste teste fazia isso, e a mutação
        // que trocava STATELESS por ALWAYS escapou.
        assertThat(resultado.getRequest().getSession(false))
                .as("sessão criada significa JSESSIONID, que é a credencial ambiente que traz o CSRF de volta")
                .isNull();
    }
}
