'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useCallback, useEffect, useRef, useState } from 'react';

import { AMBIENTE, DIFICULDADE, LUMINOSIDADE, precoEmReal } from '@/lib/rotulos';
import { precoDaUrl } from '@/lib/vitrine';

import { FiltroDePreco } from './FiltroDePreco';

/**
 * Abaixo desta largura o painel vira gaveta sobreposta.
 *
 * É o `lg` do Tailwind (1024px), que é o que decide, nas classes lá embaixo,
 * qual dos dois aparece. Os dois precisam concordar: se divergirem, existe uma
 * faixa de larguras em que a página trava sem gaveta nenhuma na frente, ou em
 * que a gaveta abre com a lista rolando atrás.
 */
const SO_TEM_GAVETA = '(max-width: 1023.98px)';

/**
 * Os filtros da vitrine.
 *
 * A primeira versão era uma coluna sempre aberta ao lado das plantas. No
 * computador funcionava; no celular, onde não existe "ao lado", a coluna ia
 * para cima da lista e **a pessoa rolava uma tela inteira de filtros antes de
 * ver a primeira planta**. Numa loja, isso é o contrário do que se quer: quem
 * chega quer ver o produto, e filtrar é o que se faz depois, se precisar.
 *
 * Agora existe uma barra fina que mostra o que está filtrado e um botão para
 * abrir o resto. As plantas ficam logo abaixo, em qualquer tamanho de tela.
 *
 * O painel abre de dois jeitos, pelo mesmo motivo de sempre (espaço):
 *
 * - **celular**: como gaveta sobre a tela, com um botão que fecha dizendo
 *   quantas plantas sobraram, para a pessoa saber o resultado antes de voltar;
 * - **computador**: empurrando o conteúdo para baixo, sem cobrir nada.
 *
 * O estado continua na URL. Isso faz o filtro sobreviver ao recarregar, permite
 * mandar o link já filtrado para alguém, e faz o botão voltar desfazer um
 * filtro em vez de sair da vitrine.
 */
export function Filtros({
  faixaDePreco,
  quantidade,
}: {
  faixaDePreco: { minimo: number; maximo: number };
  /** Quantas plantas o filtro atual encontrou, mostrado no botão da gaveta. */
  quantidade: number;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [aberto, setAberto] = useState(false);
  const gavetaRef = useRef<HTMLDivElement>(null);
  const botaoRef = useRef<HTMLButtonElement>(null);

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

  const ativos = filtrosAtivos(params);

  // Com a gaveta aberta, a página atrás não pode rolar junto: o dedo desliza a
  // lista de plantas em vez do conteúdo da gaveta.
  //
  // **Só onde a gaveta existe.** A primeira versão travava sempre, e no
  // computador, onde o painel é embutido e não cobre nada, o efeito era a barra
  // de rolagem sumir da página inteira. Medido em 1440px: barra de 15px com o
  // painel fechado, 0 com ele aberto, e o conteúdo pulando 7px para o lado
  // porque o container recentraliza quando a barra some.
  //
  // A consulta acompanha o redimensionamento em vez de ser lida uma vez só,
  // senão abrir no celular e girar para paisagem deixa a trava presa.
  useEffect(() => {
    if (!aberto) return;

    const consulta = window.matchMedia(SO_TEM_GAVETA);
    const antes = document.body.style.overflow;

    const aplicar = () => {
      document.body.style.overflow = consulta.matches ? 'hidden' : antes;
    };

    aplicar();
    consulta.addEventListener('change', aplicar);

    return () => {
      consulta.removeEventListener('change', aplicar);
      document.body.style.overflow = antes;
    };
  }, [aberto]);

  // Escape fecha, como qualquer camada sobreposta.
  useEffect(() => {
    if (!aberto) return;
    function aoTeclar(e: KeyboardEvent) {
      if (e.key === 'Escape') setAberto(false);
    }
    document.addEventListener('keydown', aoTeclar);
    return () => document.removeEventListener('keydown', aoTeclar);
  }, [aberto]);

  // Ao abrir, o foco entra na gaveta; ao fechar, volta para o botão.
  //
  // A gaveta se declara `aria-modal`, e isso faz o leitor de tela esconder o
  // resto da página. Com o foco parado no botão de fora, a pessoa ficava com o
  // cursor num lugar que o leitor considera inexistente, e o Tab passeava pelos
  // 31 elementos da vitrine atrás, que o leitor não anunciava mais.
  useEffect(() => {
    if (!aberto) return;

    const gaveta = gavetaRef.current;
    if (!gaveta) return;

    primeiroFocavel(gaveta)?.focus();

    return () => {
      // Sem devolver, o foco volta para o começo da página e quem navega por
      // teclado recomeça do cabeçalho toda vez que fecha um filtro.
      //
      // O destino vem da referência ao botão, e não de `document.activeElement`
      // guardado na abertura: clicar com o mouse não necessariamente foca o
      // botão, e nesse caso o que seria guardado é o `<body>`.
      botaoRef.current?.focus();
    };
  }, [aberto]);

  // Os mesmos grupos servem a gaveta do celular e ao painel do computador.
  // Devolver a lista, e não um elemento pronto, deixa cada lado escolher o
  // próprio arranjo: empilhado num, em colunas no outro.
  const grupos = (
    <>
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

      <Grupo titulo="Quanto quer gastar">
        <FiltroDePreco
          minimo={faixaDePreco.minimo}
          maximo={faixaDePreco.maximo}
          valor={precoDaUrl(params.get('maxPrice'))}
          aoEscolher={(v) => aplicar('maxPrice', v === null ? null : String(v))}
        />
      </Grupo>

      <Grupo titulo="Outros">
        <div className="space-y-2">
          <Marcador
            rotulo="Segura para cães e gatos"
            marcado={params.get('petSafe') === 'true'}
            aoMudar={(m) => aplicar('petSafe', m ? 'true' : null)}
          />
          <Marcador
            rotulo="Só as disponíveis"
            marcado={params.get('onlyAvailable') === 'true'}
            aoMudar={(m) => aplicar('onlyAvailable', m ? 'true' : null)}
          />
        </div>
      </Grupo>
    </>
  );

  return (
    <div className="mb-6">
      {/* A barra: o que está filtrado, e como mexer nisso. */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          ref={botaoRef}
          type="button"
          onClick={() => setAberto((a) => !a)}
          aria-expanded={aberto}
          // `aria-controls` é referência por id: apontar para um painel que só
          // existe depois de aberto faz o leitor de tela anunciar um alvo que
          // não está lá.
          aria-controls={aberto ? 'painel-de-filtros' : undefined}
          className="flex items-center gap-2 rounded-full border border-stone-300 bg-white px-4 py-2
                     text-sm font-medium text-stone-700 transition hover:border-stone-400
                     focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
        >
          <svg viewBox="0 0 16 16" aria-hidden="true" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round">
            <path d="M2 4h12M4.5 8h7M7 12h2" />
          </svg>
          Filtrar
          {ativos.length > 0 && (
            <span className="rounded-full bg-emerald-600 px-1.5 text-xs font-semibold text-white">
              {ativos.length}
            </span>
          )}
        </button>

        {/* Os filtros escolhidos, removíveis um a um: sem isso, desfazer exige
            abrir o painel e caçar qual estava marcado. */}
        {ativos.map((f) => (
          <button
            key={f.chave}
            type="button"
            onClick={() => aplicar(f.chave, null)}
            className="flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50
                       py-2 pl-3 pr-2 text-sm text-emerald-900 transition hover:bg-emerald-100
                       focus:outline-none focus:ring-2 focus:ring-emerald-500/30"
            aria-label={`Remover filtro ${f.rotulo}`}
          >
            {f.rotulo}
            <svg viewBox="0 0 16 16" aria-hidden="true" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <path d="m4 4 8 8M12 4l-8 8" />
            </svg>
          </button>
        ))}

        {ativos.length > 1 && (
          <button
            type="button"
            onClick={() => router.push('/')}
            className="text-sm text-stone-500 underline transition hover:text-stone-800"
          >
            limpar tudo
          </button>
        )}
      </div>

      {/* Computador: o painel empurra o conteúdo, sem cobrir a vitrine. */}
      {aberto && (
        <div
          id="painel-de-filtros"
          className="mt-4 hidden rounded-2xl border border-stone-200 bg-white p-6 lg:block"
        >
          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">{grupos}</div>
        </div>
      )}

      {/* Celular: gaveta por cima, com a lista intacta atrás. */}
      {aberto && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-stone-900/40"
            onClick={() => setAberto(false)}
            aria-hidden="true"
          />

          <div
            ref={gavetaRef}
            role="dialog"
            aria-modal="true"
            aria-label="Filtros"
            onKeyDown={prenderOTab}
            className="absolute inset-x-0 bottom-0 max-h-[85vh] overflow-auto rounded-t-3xl bg-white
                       p-6 pb-28 shadow-2xl"
          >
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-stone-900">Filtrar</h2>
              <button
                type="button"
                onClick={() => setAberto(false)}
                aria-label="Fechar filtros"
                className="rounded-full p-2 text-stone-500 transition hover:bg-stone-100"
              >
                <svg viewBox="0 0 16 16" aria-hidden="true" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <path d="m4 4 8 8M12 4l-8 8" />
                </svg>
              </button>
            </div>

            <div className="space-y-6">{grupos}</div>

            {/* Fixo no rodapé da gaveta: dizer quantas plantas sobraram evita
                fechar, olhar, e abrir de novo para ajustar. */}
            <div className="fixed inset-x-0 bottom-0 border-t border-stone-200 bg-white p-4">
              <button
                type="button"
                onClick={() => setAberto(false)}
                className="w-full rounded-xl bg-emerald-600 px-6 py-3 font-semibold text-white
                           transition hover:bg-emerald-700"
              >
                Ver {quantidade} {quantidade === 1 ? 'planta' : 'plantas'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/** O que está filtrado agora, já em português e pronto para virar chip. */
function filtrosAtivos(params: URLSearchParams) {
  const ativos: { chave: string; rotulo: string }[] = [];

  const luz = params.get('light');
  if (luz && luz in LUMINOSIDADE) {
    ativos.push({ chave: 'light', rotulo: LUMINOSIDADE[luz as keyof typeof LUMINOSIDADE] });
  }

  const ambiente = params.get('environment');
  if (ambiente && ambiente in AMBIENTE) {
    ativos.push({ chave: 'environment', rotulo: AMBIENTE[ambiente as keyof typeof AMBIENTE] });
  }

  const dificuldade = params.get('difficulty');
  if (dificuldade && dificuldade in DIFICULDADE) {
    ativos.push({ chave: 'difficulty', rotulo: DIFICULDADE[dificuldade as keyof typeof DIFICULDADE] });
  }

  const teto = precoDaUrl(params.get('maxPrice'));
  if (teto !== null) ativos.push({ chave: 'maxPrice', rotulo: `Até ${precoEmReal(teto)}` });

  if (params.get('petSafe') === 'true') {
    ativos.push({ chave: 'petSafe', rotulo: 'Segura para pets' });
  }

  if (params.get('onlyAvailable') === 'true') {
    ativos.push({ chave: 'onlyAvailable', rotulo: 'Só disponíveis' });
  }

  return ativos;
}

function Grupo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div>
      <h3 className="mb-3 text-sm font-medium text-stone-500">{titulo}</h3>
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
            className={`rounded-full border px-3.5 py-2 text-sm transition ${
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

function Marcador({
  rotulo,
  marcado,
  aoMudar,
}: {
  rotulo: string;
  marcado: boolean;
  aoMudar: (marcado: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 text-sm text-stone-700">
      <input
        type="checkbox"
        checked={marcado}
        onChange={(e) => aoMudar(e.target.checked)}
        className="h-4 w-4 rounded border-stone-300 text-emerald-600 focus:ring-emerald-500"
      />
      {rotulo}
    </label>
  );
}

/** O que o Tab visita, na ordem em que visita. */
const FOCAVEIS = 'a[href], button:not([disabled]), input:not([disabled]), select, textarea, [tabindex]:not([tabindex="-1"])';

function focaveisDentro(container: HTMLElement): HTMLElement[] {
  return [...container.querySelectorAll<HTMLElement>(FOCAVEIS)];
}

function primeiroFocavel(container: HTMLElement): HTMLElement | undefined {
  return focaveisDentro(container)[0];
}

/**
 * Faz o Tab dar a volta dentro da gaveta em vez de sair por baixo dela.
 *
 * O navegador não sabe que a gaveta é modal: `aria-modal` fala com o leitor de
 * tela, não com a ordem de tabulação. Sem isto, o Tab sai da gaveta e continua
 * pelos elementos da vitrine que o leitor acabou de esconder, e a pessoa fica
 * navegando por links que, para ela, não existem mais.
 */
function prenderOTab(evento: React.KeyboardEvent<HTMLDivElement>) {
  if (evento.key !== 'Tab') return;

  const focaveis = focaveisDentro(evento.currentTarget);
  if (focaveis.length === 0) return;

  const primeiro = focaveis[0];
  const ultimo = focaveis[focaveis.length - 1];
  const atual = document.activeElement;

  if (evento.shiftKey && atual === primeiro) {
    evento.preventDefault();
    ultimo.focus();
    return;
  }

  if (!evento.shiftKey && atual === ultimo) {
    evento.preventDefault();
    primeiro.focus();
  }
}
