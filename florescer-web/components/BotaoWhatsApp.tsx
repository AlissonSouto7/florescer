import type { Planta } from '@/lib/api';

/**
 * Leva a conversa para o WhatsApp já sabendo qual planta.
 *
 * Sem a mensagem pronta, a vendedora recebe "olá" sem contexto e a conversa
 * começa do zero: qual planta, qual preço, ainda tem. Com ela, a primeira
 * mensagem já diz tudo isso.
 *
 * O número vem de configuração, nunca do código: é um número pessoal, e
 * repositório público é lugar onde ninguém deveria encontrá-lo.
 *
 * Este é um componente de servidor, então a leitura acontece a cada
 * renderização, no servidor. Isso é o que permite trocar o número mexendo no
 * ambiente do container, sem reconstruir a imagem, e é por isso que a variável
 * não tem o prefixo `NEXT_PUBLIC_`: esse prefixo grava o valor dentro do
 * JavaScript durante o build, e o número ficaria congelado na imagem.
 */

function precoEmReal(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
}

export function BotaoWhatsApp({ planta }: { planta: Planta }) {
  const numero = process.env.WHATSAPP_NUMBER ?? '';

  // Sem número configurado, o botão não aparece. Um link para wa.me sem número
  // abre uma página de erro do WhatsApp, o que é pior que não ter botão.
  if (!numero) return null;

  // Planta indisponível ou sem estoque não recebe botão: o pedido chegaria para
  // uma venda que a vendedora não pode atender.
  const disponivel = planta.availability && planta.quantityStock > 0;
  if (!disponivel) {
    return (
      <p className="rounded-lg bg-stone-100 px-4 py-3 text-center text-sm text-stone-600">
        Esta planta está indisponível no momento.
      </p>
    );
  }

  const mensagem = `Olá! Tenho interesse na ${planta.name} (${precoEmReal(planta.price)}) que vi no site.`;
  // encodeURIComponent é obrigatório: sem ele, acento e quebra de linha
  // corrompem a mensagem, e o "&" de um nome cortaria o texto ao meio.
  const link = `https://wa.me/${numero}?text=${encodeURIComponent(mensagem)}`;

  return (
    <a
      href={link}
      target="_blank"
      // noopener impede que a aba aberta acesse esta janela pelo window.opener.
      rel="noopener noreferrer"
      className="flex w-full items-center justify-center gap-2 rounded-lg bg-emerald-600 px-6 py-3
                 font-semibold text-white transition hover:bg-emerald-700
                 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
    >
      <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5 fill-current">
        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51l-.57-.01c-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884a9.82 9.82 0 016.988 2.896 9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.885-9.885 9.885" />
      </svg>
      Comprar pelo WhatsApp
    </a>
  );
}
