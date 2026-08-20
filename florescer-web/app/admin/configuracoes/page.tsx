import Link from 'next/link';

import { FormularioDaLoja } from '@/components/FormularioDaLoja';
import { buscarDadosDaLoja } from '@/lib/loja';

export const metadata = { title: 'Dados da loja' };

// Sempre lido na hora: um valor em cache aqui faria a vendedora abrir a tela e
// ver o número antigo, achar que não salvou, e salvar de novo por cima.
export const dynamic = 'force-dynamic';

/**
 * Os dados da loja, editáveis por quem vende.
 *
 * Os valores atuais são buscados no servidor e entregues prontos ao formulário.
 * Buscá-los no cliente faria a tela abrir com os campos vazios e preenchê-los
 * um instante depois, o que parece perda de dados para quem está olhando.
 */
export default async function Configuracoes() {
  const dados = await buscarDadosDaLoja();

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <Link href="/admin" className="mb-6 inline-block text-sm text-emerald-700 hover:underline">
        &larr; Voltar para minhas plantas
      </Link>

      <h1 className="text-2xl font-bold text-stone-900">Dados da loja</h1>
      <p className="mt-1 mb-8 text-stone-600">
        O que aparece para quem visita, e para onde os pedidos vão.
      </p>

      <FormularioDaLoja inicial={dados} />
    </main>
  );
}
