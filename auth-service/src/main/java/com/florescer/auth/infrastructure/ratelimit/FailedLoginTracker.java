package com.florescer.auth.infrastructure.ratelimit;

/**
 * Conta erros de senha por conta, e deixa a senha certa passar sempre.
 *
 * <p>Existe porque limitar por endereço não resolve o problema sozinho, e as
 * duas formas de limitar por endereço falham de um jeito ou de outro:
 *
 * <ul>
 *   <li><b>pelo endereço da conexão</b>: em produção o navegador fala com o
 *       site, e o site repassa, então toda tentativa do mundo chega do mesmo
 *       lugar. Dez senhas erradas de um desconhecido trancam a vendedora;
 *   <li><b>pelo {@code X-Forwarded-For}</b>: o cliente também manda esse
 *       cabeçalho. Medido neste projeto: quinze tentativas trocando o valor a
 *       cada uma, e <b>nenhum 429</b>. O limite deixa de existir.
 * </ul>
 *
 * <p>A conta alvo, essa, o atacante não escolhe: para descobrir a senha da
 * vendedora ele precisa tentar contra a conta dela. Contar por aí resiste à
 * troca de endereço.
 *
 * <p>E a regra que evita transformar a proteção em bloqueio: <b>a contagem só
 * recusa quem erra</b>. A verificação da senha acontece de qualquer forma, e
 * quem acerta entra mesmo com o contador estourado. Assim um estranho não
 * consegue trancar a vendedora fora do painel, que é exatamente o que um limite
 * puro por tentativa faz.
 *
 * <p>O que isto <b>não</b> resolve: um volume absurdo de tentativas continua
 * custando uma verificação de BCrypt cada, e BCrypt é caro de propósito. Contra
 * isso vale o limite por endereço, que segue no lugar como guarda de volume.
 */
public interface FailedLoginTracker {

    /** Se a conta já passou do limite de erros na janela atual. */
    boolean isBlocked(String account);

    /** Registra um erro de senha para a conta. */
    void recordFailure(String account);

    /** Zera a contagem: quem entrou provou que é dono da conta. */
    void recordSuccess(String account);

    /** Quantos segundos faltam para a janela da conta expirar. */
    long secondsUntilReset(String account);
}
