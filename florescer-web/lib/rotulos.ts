import type { Ambiente, Dificuldade, Luminosidade, Rega } from './api';

/**
 * Traduz os valores da API para o que a pessoa lê na tela.
 *
 * A API fala `MEIA_SOMBRA` porque enum precisa de valor estável. Quem visita a
 * loja lê "meia sombra". Manter a tradução num lugar só evita que a mesma opção
 * apareça escrita de três jeitos em três telas.
 */

export const LUMINOSIDADE: Record<Luminosidade, string> = {
  SOL_PLENO: 'Sol pleno',
  MEIA_SOMBRA: 'Meia sombra',
  SOMBRA: 'Sombra',
};

/** O detalhe explica o que a opção significa na prática, para quem não sabe. */
export const LUMINOSIDADE_DETALHE: Record<Luminosidade, string> = {
  SOL_PLENO: 'Aguenta sol direto boa parte do dia',
  MEIA_SOMBRA: 'Claridade sim, sol direto não',
  SOMBRA: 'Vive bem longe da janela',
};

export const REGA: Record<Rega, string> = {
  DIARIA: 'Todo dia',
  DUAS_A_TRES_VEZES_SEMANA: '2 a 3 vezes por semana',
  SEMANAL: 'Uma vez por semana',
  QUINZENAL: 'A cada 15 dias',
  MENSAL: 'Uma vez por mês',
};

export const AMBIENTE: Record<Ambiente, string> = {
  INTERNO: 'Dentro de casa',
  EXTERNO: 'Área externa',
  AMBOS: 'Dentro ou fora',
};

export const DIFICULDADE: Record<Dificuldade, string> = {
  FACIL: 'Fácil de cuidar',
  MEDIO: 'Cuidado moderado',
  DIFICIL: 'Exige experiência',
};

export function precoEmReal(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
}

/** "40 cm" ou "1,2 m", porque 120 cm é mais difícil de imaginar que 1,2 m. */
export function altura(cm: number | null): string | null {
  if (cm === null) return null;
  if (cm >= 100) return `${(cm / 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} m`;
  return `${cm} cm`;
}
