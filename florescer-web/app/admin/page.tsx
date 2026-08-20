'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';

import { excluirPlanta, listarPlantas, type Planta } from '@/lib/api';
import { caminhoDaImagem, precoEmReal } from '@/lib/rotulos';
import { ehAdmin, esquecerToken, expirado, lerToken } from '@/lib/sessao';

/**
 * O que a vendedora vê: as plantas dela, com editar e excluir.
 *
 * A tela esconde o que a pessoa não pode fazer, mas isso é conforto, não
 * segurança: quem protege é o `@PreAuthorize` no backend, que continua valendo
 * para quem chamar a API direto.
 */
export default function Painel() {
  const router = useRouter();
  const [plantas, setPlantas] = useState<Planta[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState<Planta | null>(null);

  const carregar = useCallback(async () => {
    try {
      // Traz as inativas também: a vendedora precisa enxergar o que escondeu da
      // vitrine para poder reativar depois.
      //
      // 50 é o teto que a API impõe, e ela recusa acima disso em vez de cortar
      // em silêncio. Pedir 100 fazia a listagem inteira falhar com 400 e a tela
      // dizer "não foi possível carregar", sem pista de que a causa era o
      // tamanho da página. Quando o catálogo passar de 50, isto precisa paginar.
      const pagina = await listarPlantas({ size: 50 });
      setPlantas(pagina.content);

      if (pagina.totalElements > pagina.content.length) {
        setErro(
          `Mostrando as primeiras ${pagina.content.length} de ${pagina.totalElements} plantas.`,
        );
      }
    } catch {
      setErro('Não foi possível carregar as plantas.');
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    const token = lerToken();

    // Verifica a validade antes de pedir dados: sem isso, a tela carrega, a
    // pessoa começa a mexer e só descobre no primeiro salvamento que a sessão
    // tinha acabado.
    if (!token || expirado(token) || !ehAdmin(token)) {
      esquecerToken();
      router.replace('/login');
      return;
    }

    carregar();
  }, [carregar, router]);

  async function confirmarExclusao() {
    if (!excluindo) return;

    const token = lerToken();
    if (!token) {
      router.replace('/login');
      return;
    }

    const resposta = await excluirPlanta(excluindo.id, token);
    setExcluindo(null);

    if (resposta.ok) {
      setPlantas((atual) => atual.filter((p) => p.id !== excluindo.id));
    } else if (resposta.status === 401) {
      router.replace('/login');
    } else {
      setErro('Não foi possível excluir a planta.');
    }
  }

  if (carregando) {
    return <p className="mx-auto max-w-5xl px-4 py-16 text-stone-500">Carregando...</p>;
  }

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-stone-900">Minhas plantas</h1>
          <p className="text-sm text-stone-600">{plantas.length} cadastradas</p>
        </div>
        <div className="flex items-center gap-2">
          {/* Discreto ao lado da ação principal: mexer nos dados da loja é raro,
              cadastrar planta é o que ela faz toda semana. */}
          <Link
            href="/admin/configuracoes"
            className="rounded-xl border border-stone-300 px-4 py-2.5 text-sm font-medium text-stone-700
                       transition hover:border-stone-400 hover:bg-stone-50"
          >
            Dados da loja
          </Link>

          <Link
            href="/admin/nova"
            className="rounded-xl bg-emerald-600 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-700"
          >
            Cadastrar planta
          </Link>
        </div>
      </div>

      {erro && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {erro}
        </p>
      )}

      {plantas.length === 0 ? (
        <div className="rounded-xl border border-dashed border-stone-300 px-6 py-16 text-center">
          <p className="font-medium text-stone-700">Nenhuma planta cadastrada ainda.</p>
          <p className="mt-1 text-sm text-stone-500">
            Cadastre a primeira e ela aparece na vitrine na hora.
          </p>
        </div>
      ) : (
        <ul className="divide-y divide-stone-200 overflow-hidden rounded-xl border border-stone-200 bg-white">
          {plantas.map((planta) => (
            <li key={planta.id} className="flex items-center gap-4 p-4">
              <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-stone-100">
                <Image src={caminhoDaImagem(planta.imageUrl)} alt="" fill sizes="64px" className="object-cover" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-stone-900">{planta.name}</p>
                <p className="text-sm text-stone-500">
                  {precoEmReal(planta.price)} &middot; {planta.quantityStock} em estoque
                  {!planta.availability && ' · fora da vitrine'}
                </p>
              </div>

              <div className="flex shrink-0 gap-2">
                <Link
                  href={`/admin/${planta.id}`}
                  className="rounded-lg border border-stone-300 px-3 py-1.5 text-sm hover:bg-stone-50"
                >
                  Editar
                </Link>
                <button
                  type="button"
                  onClick={() => setExcluindo(planta)}
                  className="rounded-lg border border-red-200 px-3 py-1.5 text-sm text-red-700 hover:bg-red-50"
                >
                  Excluir
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {/* A confirmação diz o nome da planta. "Tem certeza?" sozinho é fácil de
          clicar no automático, e excluir não tem volta. */}
      {excluindo && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-stone-900/50 p-4"
          role="dialog"
          aria-modal="true"
        >
          <div className="w-full max-w-sm rounded-xl bg-white p-6">
            <h2 className="text-lg font-semibold text-stone-900">
              Excluir {excluindo.name}?
            </h2>
            <p className="mt-2 text-sm text-stone-600">
              A planta sai da vitrine e não dá para desfazer.
            </p>
            <div className="mt-6 flex gap-3">
              <button
                type="button"
                onClick={confirmarExclusao}
                className="flex-1 rounded-lg bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700"
              >
                Excluir
              </button>
              <button
                type="button"
                onClick={() => setExcluindo(null)}
                className="flex-1 rounded-lg border border-stone-300 px-4 py-2 hover:bg-stone-50"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
