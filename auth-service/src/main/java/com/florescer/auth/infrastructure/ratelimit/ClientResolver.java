package com.florescer.auth.infrastructure.ratelimit;

import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

/**
 * De quem é a tentativa, para o limite valer por cliente e não por servidor.
 *
 * <p>Isto existe por causa de uma falha medida: em produção o navegador não fala
 * com este serviço, fala com o site, e é o site que repassa. Do lado de cá,
 * então, <b>toda tentativa de login do mundo chega do mesmo endereço</b>, o do
 * contêiner do proxy. Com o limite por endereço de conexão, o resultado era uma
 * cota só para todos: dez senhas erradas de um desconhecido qualquer trancavam a
 * vendedora por um minuto, e repetir isso a cada minuto a mantinha fora do
 * painel indefinidamente. Custa dez requisições e não exige conta nenhuma.
 *
 * <p>A saída é o {@code X-Forwarded-For}, com o cuidado que ele exige: <b>o
 * cliente também pode mandar esse cabeçalho</b>. Quem lê o primeiro valor da
 * lista está lendo o que o atacante escreveu, e aí ele troca de valor a cada
 * tentativa e o limite deixa de existir. Duas regras impedem isso:
 *
 * <ol>
 *   <li>o cabeçalho só é considerado quando a conexão vem de um proxy conhecido,
 *       declarado na configuração. De qualquer outra origem, vale o endereço da
 *       conexão;
 *   <li>o valor lido é contado <b>da direita para a esquerda</b>, pulando um
 *       endereço por proxy confiável. O que está à direita foi escrito por quem
 *       recebeu a conexão, e não por quem a fez.
 * </ol>
 *
 * <p>Sem proxy configurado o comportamento é o antigo, que é o certo para quem
 * expõe o serviço direto: nesse caso o endereço da conexão já é o do cliente, e
 * confiar no cabeçalho seria o buraco.
 *
 * <h2>O que isto NÃO garante, e foi medido</h2>
 *
 * <p>As duas regras acima protegem contra quem fala direto com este serviço.
 * Elas não protegem contra quem fala com o <b>site</b>, porque <b>o Next
 * repassa o {@code X-Forwarded-For} do cliente sem sobrescrever</b>. Medido em
 * 20/08/2026 contra a stack de pé: quinze tentativas de login pelo site,
 * trocando o valor do cabeçalho a cada uma, e <b>nenhum 429</b>.
 *
 * <p>Ou seja: enquanto não houver na borda um proxy que <b>escreva</b> esse
 * cabeçalho (o nginx faz isso com {@code $proxy_add_x_forwarded_for}), este
 * limite vale contra tráfego desatento, e não contra quem está tentando de
 * propósito.
 *
 * <p>Quem protege a senha, então, é a contagem de erros por conta
 * ({@link FailedLoginTracker}), que não depende de cabeçalho nenhum: o atacante
 * escolhe o endereço que quiser, mas não escolhe outra conta para adivinhar a
 * senha da vendedora. Este resolvedor continua no lugar por dois motivos: ele
 * segura volume desatento, e passa a ser correto no dia em que a borda existir,
 * sem mudar código.
 */
public class ClientResolver {

    private static final String CABECALHO = "X-Forwarded-For";

    /** Endereços cujo {@code X-Forwarded-For} vale a pena ler. */
    private final Set<String> proxiesConfiaveis;

    /**
     * Quantos proxies existem entre o cliente e este serviço.
     *
     * <p>Com o site na frente é 1. Com um túnel ou CDN antes do site é 2, e
     * errar para menos faz todos os visitantes voltarem a dividir a mesma cota,
     * agora com o endereço do túnel.
     */
    private final int saltosConfiaveis;

    public ClientResolver(Set<String> proxiesConfiaveis, int saltosConfiaveis) {
        this.proxiesConfiaveis = Set.copyOf(proxiesConfiaveis);
        this.saltosConfiaveis = Math.max(1, saltosConfiaveis);
    }

    /** A chave do limite: o cliente, na medida em que dá para saber quem é. */
    public String resolve(HttpServletRequest requisicao) {
        String daConexao = requisicao.getRemoteAddr();

        if (!proxiesConfiaveis.contains(daConexao)) {
            return daConexao;
        }

        List<String> cadeia = cadeiaDeEncaminhamento(requisicao);
        if (cadeia.isEmpty()) {
            return daConexao;
        }

        // Da direita para a esquerda: a última posição foi escrita pelo proxy
        // mais próximo daqui, que é o único elo que não veio do cliente.
        int posicao = cadeia.size() - saltosConfiaveis;
        if (posicao < 0) {
            // A cadeia é mais curta que o esperado. Cair no endereço da conexão
            // agrupa demais, mas é o único valor que ninguém de fora escreveu:
            // pegar o que sobrou seria aceitar um endereço escolhido por quem
            // está tentando.
            return daConexao;
        }

        return cadeia.get(posicao);
    }

    private List<String> cadeiaDeEncaminhamento(HttpServletRequest requisicao) {
        String bruto = requisicao.getHeader(CABECALHO);
        if (bruto == null || bruto.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(bruto.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                // Um limite de tamanho aqui não é firula: o cabeçalho é escolhido
                // por quem chama, e uma lista enorme viraria trabalho por
                // requisição numa rota que ainda nem autenticou.
                .filter(valor -> valor.length() <= 64)
                .limit(16)
                .toList();
    }
}
