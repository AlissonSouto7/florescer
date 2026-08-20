import type { Pagina, Planta } from './api';

/**
 * Por que a vitrine está vazia.
 *
 * Vazio não é uma coisa só, e tratar como se fosse produz mentira na tela. Com
 * `?page=999` a loja mostrava "Nenhuma planta com esses filtros. Tente afrouxar
 * um deles" **sem filtro nenhum aplicado**: não havia o que afrouxar, não havia
 * chip para remover, e nenhum caminho de volta na página. A pessoa ficava numa
 * loja aparentemente sem produtos, sendo que havia doze.
 *
 * Endereço assim não é hipótese de laboratório: sai de link compartilhado,
 * histórico do navegador e página que a vendedora tirou do ar.
 */
export type MotivoDoVazio = 'filtros' | 'pagina-inexistente' | 'loja-sem-plantas';

export function motivoDoVazio(
  pagina: Pagina<Planta>,
  temFiltroAtivo: boolean,
): MotivoDoVazio | null {
  if (pagina.content.length > 0) return null;

  // Existe resultado, só não nesta página: o número pedido passou do fim.
  if (pagina.totalElements > 0) return 'pagina-inexistente';

  // Sem resultado e sem filtro: não é o filtro que está apertado, é a loja que
  // ainda não tem o que mostrar. Mandar "afrouxe um filtro" aqui é pedir uma
  // ação impossível.
  return temFiltroAtivo ? 'filtros' : 'loja-sem-plantas';
}

/** O texto de cada caso, com o que fazer a seguir. */
export const TEXTO_DO_VAZIO: Record<MotivoDoVazio, { titulo: string; detalhe: string }> = {
  filtros: {
    titulo: 'Nenhuma planta com esses filtros.',
    detalhe: 'Tente afrouxar um deles: talvez o preço ou a luminosidade.',
  },
  'pagina-inexistente': {
    titulo: 'Esta página não existe mais.',
    detalhe: 'O catálogo mudou desde que este link foi criado.',
  },
  'loja-sem-plantas': {
    titulo: 'A loja ainda não tem plantas cadastradas.',
    detalhe: 'Volte em breve: o catálogo é atualizado com frequência.',
  },
};

/**
 * O teto de preço que veio da URL, ou nada quando o que está lá não é preço.
 *
 * A URL é escrita por quem quiser, e o estrago de aceitar qualquer coisa
 * apareceu em quatro lugares diferentes: `?maxPrice=abc` virava `NaN` e a barra
 * anunciava "Até R$ abc" com o filtro não aplicado; o controle de faixa dizia
 * `aria-valuetext="Até R$ NaN"` para quem usa leitor de tela; e `?maxPrice=-5`
 * chegava à API como filtro válido, esvaziando a vitrine e mostrando "nenhuma
 * planta com esses filtros" sem chip nenhum na tela para remover.
 *
 * Ler num lugar só é o que mantém a barra, o controle e a chamada da API
 * concordando sobre o que está filtrado.
 */
export function precoDaUrl(bruto: string | null | undefined): number | null {
  if (!bruto) return null;

  const valor = Number(bruto);
  // Zero explícito não é filtro, é catálogo vazio; negativo não é preço.
  return Number.isFinite(valor) && valor > 0 ? valor : null;
}

/**
 * O número da página, contado a partir de zero.
 *
 * Página negativa ou com texto no lugar do número vira a primeira, em vez de
 * seguir para a API e virar `400` ou lista vazia sem explicação.
 */
export function paginaDaUrl(bruto: string | null | undefined): number {
  if (!bruto) return 0;

  const valor = Number(bruto);
  if (!Number.isInteger(valor) || valor < 0) return 0;
  return valor;
}
