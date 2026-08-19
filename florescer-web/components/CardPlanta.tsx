import Image from 'next/image';
import Link from 'next/link';

import type { Planta } from '@/lib/api';
import { LUMINOSIDADE, altura, precoEmReal } from '@/lib/rotulos';

/**
 * A planta na vitrine.
 *
 * Mostra o que decide o clique: foto, nome, preço, tamanho, luz e se é segura
 * para animais. O resto fica para a página de detalhe, senão o cartão vira um
 * bloco de texto e a vitrine deixa de ser navegável no celular.
 */
export function CardPlanta({ planta }: { planta: Planta }) {
  const esgotada = !planta.availability || planta.quantityStock <= 0;
  const tamanho = altura(planta.heightCm);

  return (
    <Link
      href={`/planta/${planta.id}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-stone-200 bg-white
                 transition hover:border-emerald-300 hover:shadow-lg
                 focus:outline-none focus:ring-2 focus:ring-emerald-500"
    >
      <div className="relative aspect-square overflow-hidden bg-stone-100">
        <Image
          src={planta.imageUrl}
          alt={planta.name}
          fill
          // Diz ao navegador quanto espaço a imagem ocupa em cada tamanho de
          // tela, para ele baixar a versão adequada em vez da maior sempre.
          sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
          className="object-cover transition duration-300 group-hover:scale-105"
        />
        {esgotada && (
          <div className="absolute inset-0 flex items-center justify-center bg-stone-900/60">
            <span className="rounded-full bg-white px-3 py-1 text-sm font-medium text-stone-700">
              Esgotada
            </span>
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <h3 className="font-semibold text-stone-900">{planta.name}</h3>

        <div className="flex flex-wrap gap-1.5 text-xs">
          {tamanho && (
            <span className="rounded-full bg-stone-100 px-2 py-0.5 text-stone-600">{tamanho}</span>
          )}
          {planta.light && (
            <span className="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">
              {LUMINOSIDADE[planta.light]}
            </span>
          )}
          {/* Só aparece quando é segura. "Tóxica" em letra pequena no cartão
              assusta sem explicar; o detalhe da planta diz isso com clareza. */}
          {planta.petSafe && (
            <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-emerald-700">
              Segura para pets
            </span>
          )}
        </div>

        <p className="mt-auto pt-2 text-lg font-bold text-stone-900">{precoEmReal(planta.price)}</p>
      </div>
    </Link>
  );
}
