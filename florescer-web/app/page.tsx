import { Suspense } from 'react';

import { CardPlanta } from '@/components/CardPlanta';
import { Filtros } from '@/components/Filtros';
import { listarPlantas, type Filtros as TipoFiltros } from '@/lib/api';

export const metadata = {
  title: 'Florescer | Plantas',
  description: 'Plantas com foto, tamanho, cuidados e preço. Escolha pela luz que você tem em casa.',
};

type Busca = Record<string, string | undefined>;

/**
 * A vitrine.
 *
 * Renderizada no servidor: o HTML já chega com as plantas, então o Google
 * consegue indexar cada uma e quem abre no celular não fica olhando um esqueleto
 * enquanto o JavaScript baixa.
 */
export default async function Vitrine({ searchParams }: { searchParams: Promise<Busca> }) {
  const busca = await searchParams;
  const filtros = paraFiltros(busca);

  let pagina;
  let faixa = { minimo: 0, maximo: 200 };
  let erro: string | null = null;

  try {
    /**
     * A faixa de preço vem do estoque, e não de valores escolhidos a dedo.
     *
     * Numa loja onde tudo custa menos de R$ 60, oferecer um limite de R$ 200
     * não ajuda ninguém, e a lista fixa de tetos que existia antes obrigava a
     * pessoa a aceitar um corte que alguém escolheu por ela.
     *
     * A segunda busca ignora os filtros de propósito: se ela os respeitasse, os
     * extremos encolheriam junto com o resultado, e arrastar o controle mudaria
     * a própria régua debaixo da mão de quem arrasta.
     */
    const [resultado, catalogo] = await Promise.all([
      listarPlantas(filtros),
      listarPlantas({ size: 50 }),
    ]);
    pagina = resultado;

    const precos = catalogo.content.map((p) => p.price);
    if (precos.length > 0) {
      faixa = {
        minimo: Math.floor(Math.min(...precos) / 5) * 5,
        maximo: Math.ceil(Math.max(...precos) / 5) * 5,
      };
    }
  } catch {
    // A vitrine é a primeira coisa que alguém vê. Se a API estiver fora, mostra
    // uma mensagem honesta em vez da tela de erro do framework.
    erro = 'Não foi possível carregar as plantas agora. Tente de novo em instantes.';
  }

  return (
    <main className="mx-auto max-w-7xl px-4 py-8">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-stone-900">Plantas</h1>
        <p className="mt-1 text-stone-600">
          Escolha pela luz que você tem em casa, pelo espaço, ou pelo que convive com seus animais.
        </p>
      </header>

      {/* Os filtros ficam acima, e não numa coluna ao lado. No celular a coluna
          empurrava a primeira planta para fora da tela, e quem chega numa loja
          quer ver o produto antes de filtrar. */}
      <Suspense fallback={<div className="mb-6 h-10 w-28 animate-pulse rounded-full bg-stone-200" />}>
        <Filtros faixaDePreco={faixa} quantidade={pagina?.totalElements ?? 0} />
      </Suspense>

      <section>
          {erro && (
            <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-amber-800">
              {erro}
            </p>
          )}

          {pagina && pagina.content.length === 0 && (
            <div className="rounded-lg border border-stone-200 bg-stone-50 px-6 py-12 text-center">
              <p className="font-medium text-stone-700">Nenhuma planta com esses filtros.</p>
              <p className="mt-1 text-sm text-stone-500">
                Tente afrouxar um deles: talvez o preço ou a luminosidade.
              </p>
            </div>
          )}

          {pagina && pagina.content.length > 0 && (
            <>
              <p className="mb-4 text-sm text-stone-500">
                {pagina.totalElements === 1
                  ? '1 planta encontrada'
                  : `${pagina.totalElements} plantas encontradas`}
              </p>

              <div className="grid grid-cols-2 gap-4 sm:gap-5 md:grid-cols-3 xl:grid-cols-4">
                {pagina.content.map((planta) => (
                  <CardPlanta key={planta.id} planta={planta} />
                ))}
              </div>

              {pagina.totalPages > 1 && (
                <Paginacao atual={pagina.number} total={pagina.totalPages} busca={busca} />
              )}
            </>
          )}
      </section>
    </main>
  );
}

function Paginacao({ atual, total, busca }: { atual: number; total: number; busca: Busca }) {
  const link = (pagina: number) => {
    const params = new URLSearchParams();
    // Mantém os filtros ao trocar de página, senão a página 2 mostraria a
    // vitrine inteira e a pessoa perderia o que tinha escolhido.
    Object.entries(busca).forEach(([chave, valor]) => {
      if (valor && chave !== 'page') params.set(chave, valor);
    });
    if (pagina > 0) params.set('page', String(pagina));
    return params.toString() ? `/?${params}` : '/';
  };

  return (
    <nav className="mt-8 flex items-center justify-center gap-2" aria-label="Páginas">
      {atual > 0 && (
        <a href={link(atual - 1)} className="rounded-lg border border-stone-300 px-4 py-2 text-sm hover:border-emerald-400">
          Anterior
        </a>
      )}
      <span className="px-3 text-sm text-stone-500">
        Página {atual + 1} de {total}
      </span>
      {atual < total - 1 && (
        <a href={link(atual + 1)} className="rounded-lg border border-stone-300 px-4 py-2 text-sm hover:border-emerald-400">
          Próxima
        </a>
      )}
    </nav>
  );
}

/** Converte o que veio na URL para os filtros da API, ignorando o inválido. */
function paraFiltros(busca: Busca): TipoFiltros {
  const numero = (valor: string | undefined) => {
    const n = Number(valor);
    return valor && Number.isFinite(n) ? n : undefined;
  };

  return {
    light: busca.light as TipoFiltros['light'],
    environment: busca.environment as TipoFiltros['environment'],
    difficulty: busca.difficulty as TipoFiltros['difficulty'],
    petSafe: busca.petSafe === 'true' ? true : undefined,
    onlyAvailable: busca.onlyAvailable === 'true' ? true : undefined,
    maxPrice: numero(busca.maxPrice),
    page: numero(busca.page) ?? 0,
    size: 12,
  };
}
