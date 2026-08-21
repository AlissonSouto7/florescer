package com.florescer.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;
import com.florescer.product.support.TestTokens;

/**
 * Os dados da loja, que a vendedora troca sozinha.
 *
 * <p>O que estes testes protegem, em ordem de estrago:
 *
 * <ul>
 *   <li><b>o número mal formado</b>: o link vira uma conversa que não existe, e
 *       ninguém descobre até um cliente tentar comprar e sumir;
 *   <li><b>o Instagram digitado de qualquer jeito</b>: as pessoas escrevem
 *       {@code @loja}, {@code loja} ou a URL inteira, e o rodapé precisa montar
 *       o mesmo link nos três casos;
 *   <li><b>quem pode alterar</b>: a leitura é pública porque a vitrine é aberta,
 *       mas escrever é de ADMIN. Uma anotação removida por engano não quebraria
 *       nenhum outro teste.
 * </ul>
 */
@AutoConfigureMockMvc
class ShopSettingsTest extends AbstractIntegrationTest {

    private static final String ROTA = "/v1/settings";

    @Autowired
    private MockMvc mockMvc;

    private String comoAdmin() {
        return "Bearer " + TestTokens.comEscopo("ADMIN");
    }

    private String corpo(String whatsapp, String cidade, String instagram, String horario) {
        return """
                {
                  "whatsappNumber": %s,
                  "deliveryCity": %s,
                  "instagramHandle": %s,
                  "openingHours": %s
                }
                """.formatted(json(whatsapp), json(cidade), json(instagram), json(horario));
    }

    private String json(String valor) {
        return valor == null ? "null" : "\"" + valor.replace("\"", "\\\"") + "\"";
    }

    // ---------------------------------------------------------------- leitura

    @Test
    @DisplayName("qualquer pessoa lê os dados da loja, porque a vitrine é aberta")
    void leituraEhPublica() throws Exception {
        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
    }

    @Test
    @DisplayName("a resposta pública não carrega dado interno")
    void respostaNaoVazaDadoInterno() throws Exception {
        // A data da última alteração é informação de dentro, e esta resposta vai
        // para qualquer visitante. O que não é necessário não se expõe.
        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    // ------------------------------------------------------------- alteração

    @Test
    @DisplayName("ADMIN grava e a vitrine passa a ler o valor novo")
    void adminGravaEAVitrineLe() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("5511987654321", "São Paulo, SP", "florescer.plantas",
                                "Segunda a sábado, das 8h às 18h")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappNumber").value("5511987654321"));

        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryCity").value("São Paulo, SP"))
                .andExpect(jsonPath("$.openingHours").value("Segunda a sábado, das 8h às 18h"));
    }

    @Test
    @DisplayName("sem token, alterar é recusado")
    void semTokenNaoAltera() throws Exception {
        mockMvc.perform(put(ROTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("5511987654321", null, null, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("conta comum autentica, mas não altera a loja")
    void contaComumNaoAltera() throws Exception {
        // 403, e não 401: ela se identificou, então autenticar de novo não muda
        // nada. A distinção é o que diz à pessoa o que fazer.
        mockMvc.perform(put(ROTA)
                        .header("Authorization", "Bearer " + TestTokens.comEscopo("BASIC"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("5511987654321", null, null, null)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------ número de contato

    @ParameterizedTest(name = "o número digitado como \"{0}\" é gravado como \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            // Como a pessoa digita           | Como precisa ficar
            "5511987654321                    | 5511987654321",
            "55 11 98765-4321                 | 5511987654321",
            "+55 (11) 98765-4321              | 5511987654321",
            "55-11-98765.4321                 | 5511987654321",
    })
    @DisplayName("o número é gravado só com dígitos, venha como vier")
    void limpaONumero(String digitado, String esperado) throws Exception {
        // O wa.me monta o link com o que estiver aqui: "(11) 98765-4321" viraria
        // wa.me/(11) 98765-4321, que abre uma página de erro do WhatsApp.
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(digitado.trim(), null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappNumber").value(esperado.trim()));
    }

    @ParameterizedTest(name = "\"{0}\" é recusado")
    @ValueSource(strings = {
            "123",                  // curto demais para ter país e DDD
            "1234567890123456",     // acima do teto do padrão E.164
            "abcdefghij",           // sem dígito nenhum sobrando
    })
    @DisplayName("número que não dá para ligar é recusado antes de gravar")
    void recusaNumeroInvalido(String invalido) throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(invalido, null, null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a mensagem do erro é escrita para quem vende, não para quem programa")
    void mensagemDeErroEhUtil() throws Exception {
        String resposta = mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("123", null, null, null)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // Ela precisa saber o que fazer, e não que uma expressão regular falhou.
        assertThat(resposta).contains("DDD");
        assertThat(resposta).doesNotContain("Pattern");
        assertThat(resposta).doesNotContain("regexp");
    }

    @Test
    @DisplayName("apagar o número é permitido: pode ser que ela ainda não tenha um")
    void aceitaNumeroVazio() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("", null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappNumber").doesNotExist());
    }

    // ------------------------------------------------------------- Instagram

    @ParameterizedTest(name = "\"{0}\" vira o perfil \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "florescer.plantas                              | florescer.plantas",
            "@florescer.plantas                             | florescer.plantas",
            "instagram.com/florescer.plantas                | florescer.plantas",
            "www.instagram.com/florescer.plantas            | florescer.plantas",
            "https://instagram.com/florescer.plantas        | florescer.plantas",
            "https://www.instagram.com/florescer.plantas/   | florescer.plantas",
    })
    @DisplayName("o Instagram é aceito de qualquer jeito e guardado como perfil")
    void normalizaOInstagram(String digitado, String esperado) throws Exception {
        // Estes são os seis jeitos que aparecem na prática. Guardar a URL
        // inteira faria o rodapé montar instagram.com/https://instagram.com/loja.
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, null, digitado.trim(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instagramHandle").value(esperado.trim()));
    }

    // ------------------------------------------------------- campos de texto

    @Test
    @DisplayName("campo em branco vira ausente, e some da tela em vez de virar rótulo vazio")
    void brancoViraAusente() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, "   ", "  ", "   ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryCity").doesNotExist())
                .andExpect(jsonPath("$.instagramHandle").doesNotExist())
                .andExpect(jsonPath("$.openingHours").doesNotExist());
    }

    @Test
    @DisplayName("espaço em volta é aparado")
    void aparaEspaco() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, "  São Paulo, SP  ", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryCity").value("São Paulo, SP"));
    }

    @Test
    @DisplayName("texto exagerado é recusado em vez de estourar a coluna")
    void recusaTextoLongoDemais() throws Exception {
        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, "x".repeat(200), null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o texto atravessa intacto, e a resposta continua sendo JSON")
    void textoAtravessaIntacto() throws Exception {
        // O que protege esta API não é rejeitar caracteres, e sim nunca produzir
        // HTML: a resposta é JSON, o valor é um dado, e quem exibe é que decide
        // como renderizar. O frontend usa React, que escapa texto por padrão,
        // e isso tem caso próprio em CardPlanta/rodapé.
        //
        // Filtrar "<" aqui daria falsa sensação de segurança e quebraria nomes
        // legítimos: uma loja pode se chamar "Casa & Jardim <3".
        String comSinais = "Casa & Jardim <3, São Paulo";

        mockMvc.perform(put(ROTA)
                        .header("Authorization", comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(null, comSinais, null, null)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // Volta igual ao que entrou: o dado não é mutilado no caminho.
                .andExpect(jsonPath("$.deliveryCity").value(comSinais));
    }
}
