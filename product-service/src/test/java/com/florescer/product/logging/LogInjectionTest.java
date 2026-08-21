package com.florescer.product.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.florescer.product.support.AbstractIntegrationTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * O log é arquivo de linhas, e quem escreve nele pode não ser você.
 *
 * <p>O caminho do recurso pedido vem inteiro do cliente. Registrado como veio,
 * um caractere de controle no meio dele corrompe a linha: quebra de linha cria
 * uma entrada nova com o texto que o cliente quiser, e tabulação quebra quem lê
 * o log com ferramenta que separa colunas.
 *
 * <p>É a issue #52, e o que ela tem de traiçoeiro é não aparecer em lugar
 * nenhum: a resposta ao cliente continua sendo um 404 correto, e o estrago fica
 * num arquivo que ninguém olha até precisar dele numa investigação, que é
 * exatamente quando ele estará corrompido.
 *
 * <h2>O que foi medido antes de escrever estes casos</h2>
 *
 * A issue dizia "potencial", e a palavra estava certa. Contra a stack de pé:
 *
 * <pre>
 * /uploads/x%0Ainjetado.png      401   quebra de linha nem chega
 * /uploads/x%0D%0Ainjetado.png   401   idem
 * /uploads/x%09tab.png           404   tabulação CHEGA ao handler
 * /uploads/x%20espaco.png        404   chega
 * </pre>
 *
 * Ou seja: a quebra de linha é barrada antes, porque o caminho deixa de casar
 * com a regra que libera {@code /uploads/**} e cai em
 * {@code anyRequest().authenticated()}. A tabulação passa. A sanitização cobre
 * as duas, e os casos abaixo fixam cada metade dessa realidade em vez de supor
 * uma só.
 */
@AutoConfigureMockMvc
class LogInjectionTest extends AbstractIntegrationTest {

    private static final char TABULACAO = '\t';
    private static final char NOVA_LINHA = '\n';
    private static final char RETORNO = '\r';

    @Autowired
    private MockMvc mockMvc;

    private ListAppender<ILoggingEvent> capturado;
    private Logger logger;

    @BeforeEach
    void capturarOLog() {
        logger = (Logger) LoggerFactory.getLogger(
                "com.florescer.product.domain.exception.GlobalExceptionHandler");
        capturado = new ListAppender<>();
        capturado.start();
        logger.addAppender(capturado);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void soltarOLog() {
        logger.detachAppender(capturado);
    }

    /** O que foi de fato escrito no log, já com os parâmetros substituídos. */
    private List<String> linhasDoLog() {
        return capturado.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("quebra de linha nem chega ao handler: a cadeia de segurança recusa antes")
    void quebraDeLinhaNemChega() throws Exception {
        mockMvc.perform(get("/uploads/x%0Ainjetado.png"));

        // Não há o que sanear porque não há o que registrar. O caso existe para
        // fixar essa defesa: se um dia o caminho passar a chegar, é aqui que
        // aparece, e aí a sanitização é quem segura.
        assertThat(linhasDoLog())
                .as("se passou a chegar, a defesa de cima mudou e vale reavaliar")
                .isEmpty();
    }

    // O caso da tabulação vive em ParaLogTest, e não aqui: o MockMvc não
    // decodifica o %09 como o servidor real decodifica, então por aqui ele
    // nunca chega ao handler e o teste passaria sem exercitar nada.

    @Test
    @DisplayName("caminho absurdamente longo é cortado antes de ir ao log")
    void caminhoLongoEhCortado() throws Exception {
        mockMvc.perform(get("/uploads/" + "a".repeat(3000) + ".png"));

        assertThat(linhasDoLog()).isNotEmpty();
        for (String linha : linhasDoLog()) {
            assertThat(linha.length())
                    .as("caminho de milhares de caracteres não diagnostica nada e enche o disco de quem guarda log")
                    .isLessThan(400);
        }
    }

    @Test
    @DisplayName("o caminho continua legível: sanear não pode virar apagar")
    void caminhoContinuaLegivel() throws Exception {
        // Sem esta verificação, saneamento agressivo passaria nos casos acima e
        // deixaria o handler inútil para investigar de verdade.
        mockMvc.perform(get("/uploads/foto-que-nao-existe.png"));

        assertThat(linhasDoLog())
                .as("sem o caminho no log, o handler deixa de servir para investigar")
                .anySatisfy(linha -> assertThat(linha).contains("foto-que-nao-existe"));
    }
}
