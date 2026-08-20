import type { Planta } from './api';
import { precoEmReal } from './rotulos';

/**
 * O link que leva a conversa para o WhatsApp, já sabendo qual planta.
 *
 * Vive fora do componente porque agora dois lugares precisam dele: o botão
 * grande da página de detalhe e o atalho no cartão da vitrine. Duplicar a
 * montagem da mensagem em dois arquivos é o caminho mais curto para eles
 * divergirem, e o defeito seria silencioso: um dos dois mandaria a vendedora
 * um texto sem preço, ou com o acento corrompido, e ninguém perceberia.
 */

/**
 * O número da loja.
 *
 * Vem por parâmetro, e não mais de variável de ambiente: quem manda agora é o
 * que a vendedora salvou na tela de dados da loja. A variável obrigava a editar
 * arquivo e reiniciar container para trocar de número, o que ela não faz.
 */
export function temNumero(numero: string | null | undefined): boolean {
  return Boolean(numero && numero.trim());
}

/** Se a planta pode ser vendida agora. */
export function podeComprar(planta: Planta): boolean {
  return planta.availability && planta.quantityStock > 0;
}

/**
 * Devolve o link de conversa, ou `null` quando não há o que oferecer.
 *
 * `null` acontece em dois casos, e os dois precisam esconder o botão: sem
 * número configurado, `wa.me/` abre uma página de erro do WhatsApp, o que é
 * pior que não ter botão; e planta esgotada geraria um pedido que a vendedora
 * não pode atender.
 */
export function linkDeCompra(planta: Planta, numero: string | null | undefined): string | null {
  if (!temNumero(numero) || !podeComprar(planta)) return null;

  const mensagem = `Olá! Tenho interesse na ${planta.name} (${precoEmReal(planta.price)}) que vi no site.`;

  // encodeURIComponent é obrigatório: sem ele, acento e quebra de linha
  // corrompem a mensagem, e o "&" de um nome cortaria o texto ao meio.
  return `https://wa.me/${numero}?text=${encodeURIComponent(mensagem)}`;
}
