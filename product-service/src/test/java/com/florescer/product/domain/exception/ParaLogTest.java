package com.florescer.product.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A regra que impede o cliente de escrever no log (issue #52).
 *
 * <p>O log é arquivo de linhas. Um caractere de controle vindo de fora corrompe
 * a linha: quebra de linha cria uma entrada nova com o texto que o cliente
 * quiser, e tabulação quebra quem lê com ferramenta que separa colunas. Nada
 * disso aparece na resposta ao cliente, que continua sendo um 404 correto; o
 * estrago fica num arquivo que ninguém olha até precisar dele numa
 * investigação, que é justamente quando ele estará corrompido.
 *
 * <p>Estes casos exercitam a regra direto, e não pela requisição, porque o
 * MockMvc não entrega caractere de controle no caminho como o servidor real
 * entrega. O teste de integração cobre o que é alcançável por lá; este cobre a
 * regra em si.
 */
class ParaLogTest {

    @Test
    @DisplayName("quebra de linha não sobrevive: é ela que forja uma entrada nova")
    void quebraDeLinhaNaoSobrevive() {
        String ataque = "foto.png\n2026-08-21 ERRO admin apagou o catalogo";

        String seguro = GlobalExceptionHandler.paraLog(ataque);

        assertThat(seguro).doesNotContain("\n").doesNotContain("\r");
        // O conteúdo continua lá, só não quebra a linha: apagar seria pior,
        // porque some com a pista de que alguém tentou.
        assertThat(seguro).contains("admin apagou o catalogo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n", "\t"})
    @DisplayName("nenhum caractere de controle passa")
    void nenhumControlePassa(String controle) {
        String seguro = GlobalExceptionHandler.paraLog("antes" + controle + "depois");

        assertThat(seguro)
                .doesNotContain("\n")
                .doesNotContain("\r")
                .doesNotContain("\t");
    }

    @Test
    @DisplayName("texto normal atravessa intacto: sanear não pode virar apagar")
    void textoNormalAtravessaIntacto() {
        // Sem este caso, uma regra que devolvesse string vazia passaria em todos
        // os outros e deixaria o log inútil para investigar.
        assertThat(GlobalExceptionHandler.paraLog("uploads/foto-da-samambaia.png"))
                .isEqualTo("uploads/foto-da-samambaia.png");
    }

    @Test
    @DisplayName("acento sobrevive, porque nome de arquivo tem acento")
    void acentoSobrevive() {
        assertThat(GlobalExceptionHandler.paraLog("uploads/orquídea-phalaenópsis.png"))
                .contains("orquídea")
                .contains("phalaenópsis");
    }

    @Test
    @DisplayName("caminho enorme é cortado, e o corte fica visível")
    void caminhoEnormeEhCortado() {
        String enorme = "a".repeat(5000);

        String seguro = GlobalExceptionHandler.paraLog(enorme);

        assertThat(seguro.length()).isLessThan(250);
        // As reticências existem para quem lê o log saber que foi cortado, em
        // vez de concluir que o caminho era aquele.
        assertThat(seguro).endsWith("...");
    }

    @Test
    @DisplayName("nulo não vira a palavra null no log")
    void nuloNaoViraNull() {
        assertThat(GlobalExceptionHandler.paraLog(null)).isEqualTo("(vazio)");
    }
}
