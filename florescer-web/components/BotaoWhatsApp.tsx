import type { Planta } from '@/lib/api';
import { linkDeCompra, temNumero } from '@/lib/whatsapp';
import { IconeWhatsApp } from './IconeWhatsApp';

/**
 * Leva a conversa para o WhatsApp já sabendo qual planta.
 *
 * Sem a mensagem pronta, a vendedora recebe "olá" sem contexto e a conversa
 * começa do zero: qual planta, qual preço, ainda tem. Com ela, a primeira
 * mensagem já diz tudo isso.
 *
 * O número vem de quem renderiza, que o leu dos dados da loja. Não fica no
 * código nem em variável de ambiente: é um número pessoal, e trocá-lo precisa
 * ser algo que a vendedora faz pela tela, sem depender de ninguém.
 *
 * Sem número, o botão some. Um link para `wa.me/` sem número abre uma página de
 * erro do WhatsApp, o que é pior que não ter botão nenhum.
 */
export function BotaoWhatsApp({ planta, numero }: { planta: Planta; numero: string | null }) {
  // Sem número, o botão some e nada é dito. Os dois casos precisam ser
  // distinguidos: dizer "indisponível" quando a loja é que está sem número
  // seria mentir sobre a planta, e a pessoa iria embora achando que acabou.
  if (!temNumero(numero)) return null;

  const link = linkDeCompra(planta, numero);

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
