import { configuracoes } from './api';

/**
 * Os dados da loja: WhatsApp, cidade, Instagram e horário.
 *
 * Antes o número vinha de variável de ambiente e o rodapé tinha texto fixo no
 * código. Trocar qualquer um exigia editar arquivo e reiniciar container, o que
 * a vendedora não faz. Agora vem da API, e ela edita pela tela.
 */

export type DadosDaLoja = {
  /** Só dígitos, com país e DDD. Nulo quando ela ainda não configurou. */
  whatsappNumber: string | null;
  deliveryCity: string | null;
  /** O perfil, sem o arroba: quem exibe decide como escrever. */
  instagramHandle: string | null;
  openingHours: string | null;
};

/** O que a loja mostra enquanto nada foi configurado. */
export const LOJA_VAZIA: DadosDaLoja = {
  whatsappNumber: null,
  deliveryCity: null,
  instagramHandle: null,
  openingHours: null,
};

/**
 * Lê os dados da loja.
 *
 * Sem cache, de propósito.
 *
 * A primeira versão guardava por 60 segundos, e o efeito foi ruim justamente
 * para quem mais importa: a vendedora salvava o número, abria a loja, via o
 * valor antigo e concluía que não tinha salvado. Medido: até 24 segundos até o
 * rodapé mudar.
 *
 * O custo de não guardar foi medido antes de decidir: 162 bytes e cerca de
 * 17 ms por página, numa chamada que não sai da máquina. Para uma loja deste
 * tamanho isso é irrelevante perto de mostrar informação de contato errada.
 */
export async function buscarDadosDaLoja(): Promise<DadosDaLoja> {
  try {
    const resposta = await fetch(configuracoes(), { cache: 'no-store' });

    if (!resposta.ok) return LOJA_VAZIA;
    return await resposta.json();
  } catch {
    // A loja fora do ar não pode derrubar a página inteira: sem estes dados o
    // rodapé fica mais curto e o botão de comprar some, e o resto continua de
    // pé. Uma vitrine sem rodapé vende; uma tela de erro, não.
    return LOJA_VAZIA;
  }
}

/** O endereço do perfil, pronto para virar link. */
export function linkDoInstagram(perfil: string | null): string | null {
  if (!perfil) return null;
  return `https://instagram.com/${perfil}`;
}

/**
 * Grava os dados da loja.
 *
 * Devolve a resposta crua, e não os dados já convertidos, porque a tela precisa
 * do status para distinguir os casos: 400 mostra a mensagem ao lado do campo,
 * 401 manda para o login, e o resto é falha de sistema.
 */
export async function salvarDadosDaLoja(dados: DadosDaLoja, token: string): Promise<Response> {
  return fetch(configuracoes(), {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(dados),
  });
}
