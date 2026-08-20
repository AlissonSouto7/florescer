import Image from 'next/image';
import Link from 'next/link';

import type { Planta } from '@/lib/api';
import { LUMINOSIDADE, altura, caminhoDaImagem, precoEmReal } from '@/lib/rotulos';
import { linkDeCompra } from '@/lib/whatsapp';
import { IconeWhatsApp } from './IconeWhatsApp';

/**
 * A planta na vitrine.
 *
 * Mostra o que decide o clique: foto, nome, preço, tamanho, luz e se é segura
 * para animais. O resto fica para a página de detalhe, senão o cartão vira um
 * bloco de texto e a vitrine deixa de ser navegável no celular.
 *
 * O atalho do WhatsApp fica aqui, e não só no detalhe, porque quem já reconhece
 * a planta pela foto não precisa abrir mais uma página para começar a conversa.
 * Ele é um link **irmão** do cartão, e não um filho: um `<a>` dentro de outro
 * `<a>` é HTML inválido, o navegador desmancha a estrutura, e o clique passa a
 * cair no link errado. Por isso o cartão é uma `<article>` com dois links
 * dentro, e a foto e o nome é que levam ao detalhe.
 */
export function CardPlanta({ planta }: { planta: Planta }) {
  const esgotada = !planta.availability || planta.quantityStock <= 0;
  const tamanho = altura(planta.heightCm);
  const whatsapp = linkDeCompra(planta);

  return (
    <article
      className="group relative flex flex-col overflow-hidden rounded-2xl border border-stone-200
                 bg-white transition hover:border-emerald-300 hover:shadow-lg hover:shadow-stone-900/5"
    >
      <div className="relative aspect-square overflow-hidden bg-stone-100">
        <Image
          src={caminhoDaImagem(planta.imageUrl)}
          alt={planta.name}
          fill
          // Diz ao navegador quanto espaço a imagem ocupa em cada tamanho de
          // tela, para ele baixar a versão adequada em vez da maior sempre.
          sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
          className="object-cover transition duration-500 group-hover:scale-105"
        />

        {esgotada && (
          <div className="absolute inset-0 flex items-center justify-center bg-stone-900/55">
            <span className="rounded-full bg-white px-3 py-1 text-sm font-medium text-stone-700">
              Esgotada
            </span>
          </div>
        )}

        {planta.petSafe && (
          <span
            className="absolute left-3 top-3 rounded-full bg-white/95 px-2.5 py-1 text-xs
                       font-medium text-emerald-700 shadow-sm backdrop-blur"
          >
            Segura para pets
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <h3 className="font-semibold leading-snug text-stone-900">
          {/* O link que cobre o cartão inteiro: o `after` estica a área de
              clique sobre a `article`, então clicar em qualquer lugar leva ao
              detalhe, sem precisar aninhar elementos dentro do link. */}
          <Link
            href={`/planta/${planta.id}`}
            className="after:absolute after:inset-0 after:content-[''] focus:outline-none
                       focus-visible:underline focus-visible:decoration-emerald-600"
          >
            {planta.name}
          </Link>
        </h3>

        <div className="flex flex-wrap gap-1.5 text-xs">
          {tamanho && (
            <span className="rounded-full bg-stone-100 px-2 py-0.5 text-stone-600">{tamanho}</span>
          )}
          {planta.light && (
            <span className="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">
              {LUMINOSIDADE[planta.light]}
            </span>
          )}
        </div>

        <div className="mt-auto flex items-center justify-between gap-2 pt-3">
          <p className="text-lg font-bold text-stone-900">{precoEmReal(planta.price)}</p>

          {whatsapp && (
            <a
              href={whatsapp}
              target="_blank"
              rel="noopener noreferrer"
              // z-10 tira este link de baixo da área de clique do cartão, senão
              // ele seria coberto e todo clique aqui abriria o detalhe.
              className="relative z-10 flex h-9 w-9 items-center justify-center rounded-full
                         bg-emerald-600 text-white transition hover:bg-emerald-700
                         focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
              aria-label={`Comprar ${planta.name} pelo WhatsApp`}
              title="Comprar pelo WhatsApp"
            >
              <IconeWhatsApp className="h-4 w-4" />
            </a>
          )}
        </div>
      </div>
    </article>
  );
}
