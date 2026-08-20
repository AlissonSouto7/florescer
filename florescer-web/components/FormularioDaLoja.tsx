'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';

import { errosDe } from '@/lib/api';
import { salvarDadosDaLoja, type DadosDaLoja } from '@/lib/loja';
import { lerToken } from '@/lib/sessao';

/**
 * Onde a vendedora edita os dados da loja.
 *
 * Escrito para quem não é técnico: cada campo diz **onde aquilo aparece** para
 * quem compra, e não o nome do dado. "WhatsApp que recebe os pedidos" explica
 * mais que "whatsappNumber", e a explicação embaixo evita a pergunta seguinte.
 *
 * O número é o campo mais importante da tela: errado, o pedido não chega em
 * ninguém e nada indica isso. Por isso ele tem exemplo, aceita qualquer
 * formatação, e mostra o aviso do backend ao lado do próprio campo.
 */
export function FormularioDaLoja({ inicial }: { inicial: DadosDaLoja }) {
  const router = useRouter();
  const [erros, setErros] = useState<Record<string, string>>({});
  const [salvando, setSalvando] = useState(false);
  const [salvou, setSalvou] = useState(false);

  async function aoEnviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setErros({});
    setSalvou(false);

    const token = lerToken();
    if (!token) {
      router.push('/login');
      return;
    }

    const form = new FormData(evento.currentTarget);
    const dados: DadosDaLoja = {
      whatsappNumber: String(form.get('whatsappNumber') ?? ''),
      deliveryCity: String(form.get('deliveryCity') ?? ''),
      instagramHandle: String(form.get('instagramHandle') ?? ''),
      openingHours: String(form.get('openingHours') ?? ''),
    };

    setSalvando(true);
    try {
      const resposta = await salvarDadosDaLoja(dados, token);

      if (resposta.ok) {
        setSalvou(true);
        // A vitrine lê estes dados no servidor, então precisa ser recarregada
        // para o rodapé e o botão de comprar mostrarem o valor novo.
        router.refresh();
        return;
      }

      if (resposta.status === 401) {
        router.push('/login');
        return;
      }

      setErros(await errosDe(resposta));
    } catch {
      setErros({ _geral: 'Não foi possível salvar. Verifique sua conexão.' });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form onSubmit={aoEnviar} className="space-y-6">
      {erros._geral && (
        <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          {erros._geral}
        </p>
      )}

      {salvou && (
        <p role="status" className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          Salvo. As mudanças já aparecem para quem visita a loja.
        </p>
      )}

      <Secao titulo="Onde os pedidos chegam">
        <Campo
          id="whatsappNumber"
          rotulo="WhatsApp que recebe os pedidos"
          dica="Com o código do país e o DDD. Pode digitar com parênteses e traço, exemplo: +55 (11) 98765-4321"
          erro={erros.whatsappNumber}
        >
          <input
            id="whatsappNumber"
            name="whatsappNumber"
            inputMode="tel"
            autoComplete="tel"
            defaultValue={inicial.whatsappNumber ?? ''}
            placeholder="55 11 98765-4321"
            className={ESTILO_INPUT}
          />
        </Campo>

        <p className="text-sm text-stone-500">
          É para este número que o botão &ldquo;Comprar pelo WhatsApp&rdquo; leva, em todas as plantas.
          Deixando em branco, o botão some da loja.
        </p>
      </Secao>

      <Secao titulo="O que aparece no rodapé">
        <Campo
          id="deliveryCity"
          rotulo="Cidade onde você entrega"
          dica="Assim quem é de longe já sabe antes de perguntar"
          erro={erros.deliveryCity}
        >
          <input
            id="deliveryCity"
            name="deliveryCity"
            defaultValue={inicial.deliveryCity ?? ''}
            placeholder="São Paulo e região"
            className={ESTILO_INPUT}
          />
        </Campo>

        <Campo
          id="instagramHandle"
          rotulo="Instagram da loja"
          dica="Pode colar o endereço todo ou só o @: os dois funcionam"
          erro={erros.instagramHandle}
        >
          <input
            id="instagramHandle"
            name="instagramHandle"
            defaultValue={inicial.instagramHandle ?? ''}
            placeholder="@florescer.plantas"
            className={ESTILO_INPUT}
          />
        </Campo>

        <Campo
          id="openingHours"
          rotulo="Horário de atendimento"
          dica="Evita que alguém mande mensagem de madrugada e ache que foi ignorado"
          erro={erros.openingHours}
        >
          <input
            id="openingHours"
            name="openingHours"
            defaultValue={inicial.openingHours ?? ''}
            placeholder="Segunda a sábado, das 8h às 18h"
            className={ESTILO_INPUT}
          />
        </Campo>

        <p className="text-sm text-stone-500">
          Campo em branco simplesmente não aparece no site.
        </p>
      </Secao>

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={salvando}
          className="rounded-xl bg-emerald-600 px-6 py-2.5 font-medium text-white transition
                     hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-stone-300"
        >
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>

        <button
          type="button"
          onClick={() => router.push('/admin')}
          className="rounded-xl border border-stone-300 px-6 py-2.5 text-stone-700 transition hover:bg-stone-50"
        >
          Voltar
        </button>
      </div>
    </form>
  );
}

const ESTILO_INPUT =
  'w-full rounded-xl border border-stone-300 px-3.5 py-2.5 text-stone-900 transition ' +
  'hover:border-stone-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/25';

function Secao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <fieldset className="space-y-4 rounded-2xl border border-stone-200 bg-white p-5">
      <legend className="px-2 text-sm font-semibold text-stone-500">{titulo}</legend>
      {children}
    </fieldset>
  );
}

function Campo({
  id,
  rotulo,
  dica,
  erro,
  children,
}: {
  id: string;
  rotulo: string;
  dica?: string;
  erro?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-stone-700">
        {rotulo}
      </label>
      {dica && <p className="mb-1.5 text-xs text-stone-500">{dica}</p>}
      {children}
      {/* role="alert" faz o leitor de tela anunciar o erro assim que ele surge. */}
      {erro && (
        <p role="alert" className="mt-1 text-sm text-red-600">
          {erro}
        </p>
      )}
    </div>
  );
}
