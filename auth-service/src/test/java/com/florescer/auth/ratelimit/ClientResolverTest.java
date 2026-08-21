package com.florescer.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.florescer.auth.infrastructure.ratelimit.ClientResolver;

/**
 * O que estes testes protegem.
 *
 * <p>Duas falhas opostas, e as duas custam caro:
 *
 * <ul>
 *   <li><b>agrupar demais</b>: em produção o navegador fala com o site, e o site
 *       repassa. Se o limite for pelo endereço da conexão, toda tentativa do
 *       mundo chega do mesmo lugar e divide a mesma cota. Dez senhas erradas de
 *       um desconhecido trancam a vendedora, e repetir a cada minuto a mantém
 *       fora do painel de graça;
 *   <li><b>confiar demais</b>: o {@code X-Forwarded-For} também pode vir do
 *       cliente. Ler o primeiro valor da lista é ler o que o atacante escreveu,
 *       e aí ele troca de endereço a cada tentativa e o limite some.
 * </ul>
 *
 * <p>Por isso os casos abaixo cobrem os dois lados, e não só o caminho feliz.
 */
class ClientResolverTest {

    private static final String PROXY = "10.0.0.2";
    private static final String CLIENTE = "203.0.113.7";

    private static MockHttpServletRequest de(String enderecoDaConexao, String encaminhado) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setRemoteAddr(enderecoDaConexao);
        if (encaminhado != null) {
            requisicao.addHeader("X-Forwarded-For", encaminhado);
        }
        return requisicao;
    }

    /** O arranjo de produção: o site na frente, um salto. */
    private static ClientResolver comProxy() {
        return new ClientResolver(Set.of(PROXY), 1);
    }

    @Nested
    @DisplayName("sem proxy configurado")
    class SemProxy {

        private final ClientResolver resolvedor = new ClientResolver(Set.of(), 1);

        @Test
        @DisplayName("usa o endereço da conexão, que é o do próprio cliente")
        void usaOEnderecoDaConexao() {
            assertThat(resolvedor.resolve(de(CLIENTE, null))).isEqualTo(CLIENTE);
        }

        @Test
        @DisplayName("ignora o cabeçalho, porque quem o mandou foi o cliente")
        void ignoraOCabecalho() {
            // Este é o caso que abriria o buraco: aceitar aqui deixaria qualquer
            // um tentar sem limite, trocando o cabeçalho a cada requisição.
            assertThat(resolvedor.resolve(de(CLIENTE, "1.1.1.1"))).isEqualTo(CLIENTE);
        }
    }

    @Nested
    @DisplayName("com o site como proxy confiável")
    class ComProxy {

        @Test
        @DisplayName("separa dois visitantes que chegam pelo mesmo proxy")
        void separaVisitantes() {
            // A falha medida: sem isto, os dois viravam a mesma chave e um
            // trancava o outro.
            String primeiro = comProxy().resolve(de(PROXY, "198.51.100.4"));
            String segundo = comProxy().resolve(de(PROXY, CLIENTE));

            assertThat(primeiro).isNotEqualTo(segundo);
            assertThat(segundo).isEqualTo(CLIENTE);
        }

        @Test
        @DisplayName("lê da direita, e não o valor que o cliente escreveu")
        void leDaDireita() {
            // O cliente mandou "9.9.9.9" de propósito; o proxy anexou o endereço
            // real. Ler da esquerda entregaria o controle da chave ao atacante.
            String chave = comProxy().resolve(de(PROXY, "9.9.9.9, " + CLIENTE));

            assertThat(chave).isEqualTo(CLIENTE);
            assertThat(chave).isNotEqualTo("9.9.9.9");
        }

        @Test
        @DisplayName("uma cadeia inteira forjada não muda a chave")
        void cadeiaForjadaNaoMuda() {
            // O atacante enche o cabeçalho tentando empurrar o valor real para
            // fora da posição lida.
            String comLixo = comProxy().resolve(de(PROXY, "1.1.1.1, 2.2.2.2, 3.3.3.3, " + CLIENTE));
            String limpo = comProxy().resolve(de(PROXY, CLIENTE));

            assertThat(comLixo).isEqualTo(limpo);
        }

        @Test
        @DisplayName("sem cabeçalho, volta para o endereço da conexão")
        void semCabecalhoVoltaParaAConexao() {
            assertThat(comProxy().resolve(de(PROXY, null))).isEqualTo(PROXY);
        }

        @Test
        @DisplayName("cabeçalho vazio não vira chave vazia")
        void cabecalhoVazio() {
            // Chave vazia juntaria todo mundo numa cota só, que é exatamente o
            // problema que este código existe para resolver.
            assertThat(comProxy().resolve(de(PROXY, "   "))).isEqualTo(PROXY);
            assertThat(comProxy().resolve(de(PROXY, " , , "))).isEqualTo(PROXY);
        }

        @Test
        @DisplayName("descarta valor absurdamente longo, que só serve para gastar CPU")
        void descartaValorLongo() {
            String enorme = "x".repeat(5000);

            assertThat(comProxy().resolve(de(PROXY, enorme))).isEqualTo(PROXY);
        }

        @Test
        @DisplayName("conexão de origem desconhecida não tem o cabeçalho lido")
        void origemDesconhecida() {
            // Alguém que alcance o serviço por fora do proxy não escolhe a
            // própria chave.
            assertThat(comProxy().resolve(de("192.0.2.55", CLIENTE))).isEqualTo("192.0.2.55");
        }
    }

    @Nested
    @DisplayName("com dois saltos, como num túnel na frente do site")
    class DoisSaltos {

        private final ClientResolver resolvedor = new ClientResolver(Set.of(PROXY), 2);

        @Test
        @DisplayName("pula o túnel e chega no visitante")
        void pulaOTunel() {
            // cliente -> túnel(198.51.100.9) -> site -> aqui
            String chave = resolvedor.resolve(de(PROXY, CLIENTE + ", 198.51.100.9"));

            assertThat(chave).isEqualTo(CLIENTE);
        }

        @Test
        @DisplayName("cadeia curta demais não vira endereço escolhido pelo cliente")
        void cadeiaCurta() {
            // Só um valor, e ele é justamente o que o cliente pode escrever.
            // Agrupar é ruim; obedecer ao atacante é pior.
            assertThat(resolvedor.resolve(de(PROXY, "9.9.9.9"))).isEqualTo(PROXY);
        }
    }
}
