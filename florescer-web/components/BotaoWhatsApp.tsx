import type { Planta } from '@/lib/api';
import { linkDeCompra, numeroDaLoja } from '@/lib/whatsapp';
import { IconeWhatsApp } from './IconeWhatsApp';

/**
 * Leva a conversa para o WhatsApp já sabendo qual planta.
 *
 * Sem a mensagem pronta, a vendedora recebe "olá" sem contexto e a conversa
 * começa do zero: qual planta, qual preço, ainda tem. Com ela, a primeira
 * mensagem já diz tudo isso.
 *
 * O número vem de configuração, nunca do código: é um número pessoal, e
 * repositório público é lugar onde ninguém deveria encontrá-lo. Este é um
 * componente de servidor, então a leitura acontece a cada renderização, e
 * trocar de número é mexer no ambiente do container, sem reconstruir a imagem.
 */
export function BotaoWhatsApp({ planta }: { planta: Planta }) {
  // Sem número configurado, o botão não aparece. Um link para wa.me sem número
  // abre uma página de erro do WhatsApp, o que é pior que não ter botão.
  if (!numeroDaLoja()) return null;

  const link = linkDeCompra(planta);

  // Planta indisponível ou sem estoque não recebe botão: o pedido chegaria para
  // uma venda que a vendedora não pode atender.
  if (!link) {
    return (
      <p className="rounded-xl bg-stone-100 px-4 py-3 text-center text-sm text-stone-600">
        Esta planta está indisponível no momento.
      </p>
    );
  }

  return (
    <a
      href={link}
      target="_blank"
      // noopener impede que a aba aberta acesse esta janela pelo window.opener.
      rel="noopener noreferrer"
      className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 px-6 py-3.5
                 font-semibold text-white shadow-sm transition hover:bg-emerald-700
                 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
    >
      <IconeWhatsApp />
      Comprar pelo WhatsApp
    </a>
  );
}
