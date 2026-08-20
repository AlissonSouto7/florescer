import type { SelectHTMLAttributes } from 'react';

/**
 * Um `select` que parece parte da loja.
 *
 * O `select` sem estilo é desenhado pelo sistema operacional: a seta, a borda e
 * a altura vêm do Windows, do macOS ou do Android, e nenhum deles combina com o
 * resto da página. Ao lado de campos arredondados e botões verdes, ele aparece
 * como um pedaço de outro programa.
 *
 * `appearance-none` remove o desenho do sistema, **e a seta some junto**. Ela
 * volta aqui como um SVG irmão, posicionado por cima. A primeira versão usava
 * a seta como imagem de fundo numa classe arbitrária do Tailwind, e o CSS não
 * chegou a ser gerado: o resultado foi um select sem seta nenhuma, que é pior
 * que a seta feia do sistema, porque nada indica que aquilo abre uma lista.
 *
 * O `pointer-events-none` na seta é o que faz clicar nela abrir a lista: sem
 * isso, o clique para no ícone e o campo parece não responder.
 *
 * O que continua sendo do sistema é a **lista aberta**: nenhum CSS a alcança. É
 * uma escolha consciente, e a certa: a lista nativa é a que funciona com leitor
 * de tela, com teclado e no celular, onde Android e iOS mostram um seletor
 * próprio que a pessoa já sabe usar. Trocá-la por uma lista feita à mão custaria
 * acessibilidade em troca de aparência.
 */
export const ESTILO_SELECT =
  'w-full appearance-none rounded-lg border border-stone-300 bg-white py-2 pl-3 pr-10 ' +
  'text-sm text-stone-900 transition cursor-pointer hover:border-stone-400 ' +
  'focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/30 ' +
  'disabled:cursor-not-allowed disabled:bg-stone-50 disabled:text-stone-400';

export function Select({ className = '', children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="relative">
      <select className={`${ESTILO_SELECT} ${className}`} {...props}>
        {children}
      </select>

      <svg
        viewBox="0 0 16 16"
        aria-hidden="true"
        className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-500"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="m4 6 4 4 4-4" />
      </svg>
    </div>
  );
}
