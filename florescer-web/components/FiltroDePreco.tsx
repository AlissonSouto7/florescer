'use client';

import { useEffect, useState } from 'react';

import { precoEmReal } from '@/lib/rotulos';

/**
 * A faixa de preço, com o valor escolhido arrastando.
 *
 * A lista fechada de tetos ("até R$ 30, até R$ 50...") tinha dois problemas:
 * obrigava a pessoa a aceitar um dos valores que alguém escolheu por ela, e não
 * dizia nada sobre o catálogo. Quem procurava algo até R$ 70 tinha que decidir
 * entre ver de menos ou ver demais.
 *
 * Aqui a faixa vai do preço da planta mais barata ao da mais cara, então os
 * extremos vêm do estoque real: numa loja onde tudo custa menos de R$ 60, não
 * faz sentido oferecer um limite de R$ 200.
 *
 * A URL só muda quando a pessoa solta o controle, e não a cada pixel: navegar
 * a cada movimento dispararia uma consulta por quadro de animação e faria a
 * lista piscar sem parar.
 */
export function FiltroDePreco({
  minimo,
  maximo,
  valor,
  aoEscolher,
}: {
  minimo: number;
  maximo: number;
  /** O teto atual, ou nulo quando não há filtro de preço. */
  valor: number | null;
  aoEscolher: (valor: number | null) => void;
}) {
  const [arrastando, setArrastando] = useState(valor ?? maximo);

  // Quando o filtro muda por fora (limpar, voltar do navegador, link recebido),
  // o controle precisa acompanhar, senão ele mostra um valor que não é o da URL.
  useEffect(() => {
    setArrastando(valor ?? maximo);
  }, [valor, maximo]);

  const semFiltro = arrastando >= maximo;
  const percorrido = maximo > minimo ? ((arrastando - minimo) / (maximo - minimo)) * 100 : 100;

  function soltar(novo: number) {
    // Arrastar até o fim significa "tanto faz", e não "até o mais caro": manter
    // o parâmetro deixaria a URL suja e o botão "limpar" aceso à toa.
    aoEscolher(novo >= maximo ? null : novo);
  }

  return (
    <div>
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-sm text-stone-600">
          {semFiltro ? 'Qualquer preço' : `Até ${precoEmReal(arrastando)}`}
        </span>
        {!semFiltro && (
          <button
            type="button"
            onClick={() => aoEscolher(null)}
            className="text-xs text-emerald-700 underline transition hover:text-emerald-800"
          >
            remover
          </button>
        )}
      </div>

      <input
        type="range"
        min={minimo}
        max={maximo}
        step={5}
        value={arrastando}
        aria-label="Preço máximo"
        aria-valuetext={semFiltro ? 'Qualquer preço' : `Até ${precoEmReal(arrastando)}`}
        onChange={(e) => setArrastando(Number(e.target.value))}
        onMouseUp={(e) => soltar(Number((e.target as HTMLInputElement).value))}
        onTouchEnd={(e) => soltar(Number((e.target as HTMLInputElement).value))}
        onKeyUp={(e) => soltar(Number((e.target as HTMLInputElement).value))}
        // O trecho já percorrido fica verde, e o resto cinza. Um degradê
        // resolve isso sem precisar de um elemento a mais por cima.
        style={{
          background: `linear-gradient(to right, #059669 ${percorrido}%, #e7e5e4 ${percorrido}%)`,
        }}
        className="mt-3 h-1.5 w-full cursor-pointer appearance-none rounded-full outline-none
                   focus-visible:ring-2 focus-visible:ring-emerald-500/40
                   [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:w-4
                   [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:rounded-full
                   [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-white
                   [&::-webkit-slider-thumb]:bg-emerald-600 [&::-webkit-slider-thumb]:shadow
                   [&::-moz-range-thumb]:h-4 [&::-moz-range-thumb]:w-4
                   [&::-moz-range-thumb]:appearance-none [&::-moz-range-thumb]:rounded-full
                   [&::-moz-range-thumb]:border-2 [&::-moz-range-thumb]:border-white
                   [&::-moz-range-thumb]:bg-emerald-600"
      />

      <div className="mt-1 flex justify-between text-xs text-stone-400">
        <span>{precoEmReal(minimo)}</span>
        <span>{precoEmReal(maximo)}</span>
      </div>
    </div>
  );
}
