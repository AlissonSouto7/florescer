'use client';

import { useEffect, useId, useRef, useState } from 'react';

export type Opcao = { valor: string; rotulo: string; detalhe?: string };

/**
 * Uma lista de opções com a cara da loja, e não a do sistema operacional.
 *
 * O `<select>` nativo tem um problema que nenhum CSS resolve: **a lista aberta
 * é desenhada pelo sistema**. No Windows ela vem azul e quadrada, e ao lado de
 * campos arredondados e verdes parece um pedaço de outro programa. `appearance`
 * alcança o campo fechado; a lista, não.
 *
 * Por isso aqui a lista é construída do zero. O custo disso é que tudo o que o
 * navegador dava de graça precisa ser reescrito, e é onde a maioria das
 * implementações caseiras falha:
 *
 * - **teclado**: setas andam, Home e End vão às pontas, Enter escolhe, Escape
 *   fecha e devolve o foco ao botão, Tab fecha sem escolher;
 * - **leitor de tela**: o botão é `combobox` com `aria-expanded` e
 *   `aria-controls`, a lista é `listbox`, cada item é `option` com
 *   `aria-selected`, e `aria-activedescendant` diz qual está em foco;
 * - **clicar fora e rolar**: fecha, como qualquer lista nativa;
 * - **foco visível**: o item sob o teclado é destacado, senão quem navega sem
 *   mouse não sabe onde está.
 *
 * No celular a lista nativa costuma ser melhor (o seletor de rolagem do iOS e
 * do Android), mas ela só aparece em `<select>` de verdade. A troca aqui é
 * consciente: a lista própria é consistente em toda tela, e o comportamento de
 * teclado e leitor de tela foi implementado por inteiro em vez de presumido.
 */
export function Select({
  opcoes,
  valor,
  aoMudar,
  rotuloAcessivel,
  name,
  id,
  className = '',
}: {
  opcoes: Opcao[];
  valor: string;
  aoMudar: (valor: string) => void;
  rotuloAcessivel: string;
  /**
   * Quando presente, o valor escolhido acompanha o formulário num campo oculto.
   *
   * Sem isso, este componente seria invisível para o `FormData`: ele é um botão
   * com uma lista, e não um `<select>`. O formulário de cadastro lê tudo pelo
   * `FormData` do evento de envio, e o campo simplesmente não chegaria à API.
   */
  name?: string;
  id?: string;
  className?: string;
}) {
  const [aberto, setAberto] = useState(false);
  const [emFoco, setEmFoco] = useState(0);
  const caixa = useRef<HTMLDivElement>(null);
  const botao = useRef<HTMLButtonElement>(null);
  const gerado = useId();
  const idBase = id ?? gerado;

  const indiceAtual = Math.max(0, opcoes.findIndex((o) => o.valor === valor));
  const escolhida = opcoes[indiceAtual];

  // Clique fora e rolagem fecham, como acontece com a lista nativa.
  useEffect(() => {
    if (!aberto) return;

    function foraDaqui(evento: MouseEvent) {
      if (!caixa.current?.contains(evento.target as Node)) setAberto(false);
    }
    function aoRolar() {
      setAberto(false);
    }

    document.addEventListener('mousedown', foraDaqui);
    window.addEventListener('scroll', aoRolar, true);
    return () => {
      document.removeEventListener('mousedown', foraDaqui);
      window.removeEventListener('scroll', aoRolar, true);
    };
  }, [aberto]);

  function abrir() {
    setEmFoco(indiceAtual);
    setAberto(true);
  }

  function escolher(indice: number) {
    aoMudar(opcoes[indice].valor);
    setAberto(false);
    botao.current?.focus();
  }

  function aoTeclar(evento: React.KeyboardEvent) {
    if (!aberto) {
      // Espaço, Enter e as setas abrem, como no select nativo.
      if ([' ', 'Enter', 'ArrowDown', 'ArrowUp'].includes(evento.key)) {
        evento.preventDefault();
        abrir();
      }
      return;
    }

    switch (evento.key) {
      case 'ArrowDown':
        evento.preventDefault();
        setEmFoco((i) => Math.min(i + 1, opcoes.length - 1));
        break;
      case 'ArrowUp':
        evento.preventDefault();
        setEmFoco((i) => Math.max(i - 1, 0));
        break;
      case 'Home':
        evento.preventDefault();
        setEmFoco(0);
        break;
      case 'End':
        evento.preventDefault();
        setEmFoco(opcoes.length - 1);
        break;
      case 'Enter':
      case ' ':
        evento.preventDefault();
        escolher(emFoco);
        break;
      case 'Escape':
        evento.preventDefault();
        setAberto(false);
        botao.current?.focus();
        break;
      case 'Tab':
        // Tab sai do campo, e sair sem escolher precisa fechar a lista.
        setAberto(false);
        break;
    }
  }

  return (
    <div ref={caixa} className={`relative ${className}`}>
      {name && <input type="hidden" name={name} value={valor} />}

      <button
        ref={botao}
        id={idBase}
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded={aberto}
        aria-controls={`${idBase}-lista`}
        aria-label={rotuloAcessivel}
        // No botão, e não na lista: este atributo precisa estar no elemento que
        // tem o foco, e o foco nunca sai daqui. Na lista ele seria ignorado, e
        // quem usa leitor de tela não ouviria a opção mudar com as setas.
        aria-activedescendant={aberto ? `${idBase}-opcao-${emFoco}` : undefined}
        onClick={() => (aberto ? setAberto(false) : abrir())}
        onKeyDown={aoTeclar}
        className="flex w-full items-center justify-between gap-2 rounded-xl border border-stone-300
                   bg-white px-4 py-2.5 text-left text-sm text-stone-900 transition
                   hover:border-stone-400 focus:border-emerald-500 focus:outline-none
                   focus:ring-2 focus:ring-emerald-500/25"
      >
        <span className="truncate">{escolhida?.rotulo}</span>
        <svg
          viewBox="0 0 16 16"
          aria-hidden="true"
          className={`h-4 w-4 shrink-0 text-stone-500 transition-transform ${aberto ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="m4 6 4 4 4-4" />
        </svg>
      </button>

      {aberto && (
        <ul
          id={`${idBase}-lista`}
          role="listbox"
          aria-label={rotuloAcessivel}
          tabIndex={-1}
          className="absolute z-30 mt-2 max-h-72 w-full overflow-auto rounded-xl border
                     border-stone-200 bg-white p-1 shadow-lg shadow-stone-900/10"
        >
          {opcoes.map((opcao, indice) => {
            const selecionada = opcao.valor === valor;
            const focada = indice === emFoco;

            return (
              <li
                key={opcao.valor}
                id={`${idBase}-opcao-${indice}`}
                role="option"
                aria-selected={selecionada}
                onClick={() => escolher(indice)}
                onMouseEnter={() => setEmFoco(indice)}
                className={`flex cursor-pointer items-center justify-between gap-3 rounded-lg
                            px-3 py-2 text-sm transition ${
                              focada ? 'bg-emerald-50 text-emerald-900' : 'text-stone-700'
                            }`}
              >
                <span className="min-w-0">
                  <span className={`block truncate ${selecionada ? 'font-semibold' : ''}`}>
                    {opcao.rotulo}
                  </span>
                  {opcao.detalhe && (
                    <span className="block truncate text-xs text-stone-500">{opcao.detalhe}</span>
                  )}
                </span>

                {selecionada && (
                  <svg
                    viewBox="0 0 16 16"
                    aria-hidden="true"
                    className="h-4 w-4 shrink-0 text-emerald-600"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="m3 8 3.5 3.5L13 5" />
                  </svg>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
