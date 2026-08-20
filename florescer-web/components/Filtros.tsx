'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useCallback } from 'react';

import { AMBIENTE, DIFICULDADE, LUMINOSIDADE } from '@/lib/rotulos';
import { Select } from './Select';

/**
 * Filtros da vitrine.
 *
 * O estado mora na URL, não em memória. Assim o filtro escolhido sobrevive ao
 * recarregar, dá para mandar o link já filtrado para alguém ("olha, essas são
 * seguras pro seu gato"), e o botão voltar do navegador desfaz um filtro em vez
 * de sair da vitrine.
 */
export function Filtros() {
  const router = useRouter();
  const params = useSearchParams();

  const aplicar = useCallback(
    (chave: string, valor: string | null) => {
      const novo = new URLSearchParams(params.toString());

      if (valor) novo.set(chave, valor);
      else novo.delete(chave);

      // Trocar um filtro volta para a primeira página: continuar na página 3 de
      // um resultado que agora tem uma página só mostraria a vitrine vazia.
      novo.delete('page');

      router.push(novo.toString() ? `/?${novo}` : '/');
    },
    [params, router],
  );

  const ativo = params.toString().replace(/&?page=\d+/, '') !== '';

  return (
    <aside className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-stone-900">Filtrar</h2>
        {ativo && (
          <button
            type="button"
            onClick={() => router.push('/')}
            className="text-sm text-emerald-700 underline hover:text-emerald-800"
          >
            limpar
          </button>
        )}
      </div>

      <Grupo titulo="Luz que recebe">
        <Opcoes
          nome="light"
          atual={params.get('light')}
          opcoes={Object.entries(LUMINOSIDADE)}
          aoEscolher={aplicar}
        />
      </Grupo>

      <Grupo titulo="Onde vai ficar">
        <Opcoes
          nome="environment"
          atual={params.get('environment')}
          opcoes={Object.entries(AMBIENTE).filter(([valor]) => valor !== 'AMBOS')}
          aoEscolher={aplicar}
        />
      </Grupo>

      <Grupo titulo="Quanto dá trabalho">
        <Opcoes
          nome="difficulty"
          atual={params.get('difficulty')}
          opcoes={Object.entries(DIFICULDADE)}
          aoEscolher={aplicar}
        />
      </Grupo>

      <Grupo titulo="Outros">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-stone-700">
          <input
            type="checkbox"
            checked={params.get('petSafe') === 'true'}
            onChange={(e) => aplicar('petSafe', e.target.checked ? 'true' : null)}
            className="h-4 w-4 rounded border-stone-300 text-emerald-600 focus:ring-emerald-500"
          />
          Segura para cães e gatos
        </label>

        <label className="mt-2 flex cursor-pointer items-center gap-2 text-sm text-stone-700">
          <input
            type="checkbox"
            checked={params.get('onlyAvailable') === 'true'}
            onChange={(e) => aplicar('onlyAvailable', e.target.checked ? 'true' : null)}
            className="h-4 w-4 rounded border-stone-300 text-emerald-600 focus:ring-emerald-500"
          />
          Só as disponíveis
        </label>
      </Grupo>

      <Grupo titulo="Até quanto quer gastar">
        <Select
          aria-label="Preço máximo"
          value={params.get('maxPrice') ?? ''}
          onChange={(e) => aplicar('maxPrice', e.target.value || null)}
        >
          <option value="">Qualquer preço</option>
          <option value="30">Até R$ 30</option>
          <option value="50">Até R$ 50</option>
          <option value="100">Até R$ 100</option>
          <option value="200">Até R$ 200</option>
        </Select>
      </Grupo>
    </aside>
  );
}

function Grupo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="mb-2 text-sm font-medium text-stone-500">{titulo}</h3>
      {children}
    </div>
  );
}

function Opcoes({
  nome,
  atual,
  opcoes,
  aoEscolher,
}: {
  nome: string;
  atual: string | null;
  opcoes: [string, string][];
  aoEscolher: (chave: string, valor: string | null) => void;
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {opcoes.map(([valor, rotulo]) => {
        const selecionado = atual === valor;
        return (
          <button
            key={valor}
            type="button"
            // Clicar na opção já escolhida desmarca: é como a pessoa espera
            // desfazer, sem precisar procurar o "limpar".
            onClick={() => aoEscolher(nome, selecionado ? null : valor)}
            aria-pressed={selecionado}
            className={`rounded-full border px-3 py-1 text-sm transition ${
              selecionado
                ? 'border-emerald-600 bg-emerald-600 text-white'
                : 'border-stone-300 bg-white text-stone-700 hover:border-emerald-400'
            }`}
          >
            {rotulo}
          </button>
        );
      })}
    </div>
  );
}
