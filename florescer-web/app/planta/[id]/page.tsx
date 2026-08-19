import Image from 'next/image';
import Link from 'next/link';
import { notFound } from 'next/navigation';

import { BotaoWhatsApp } from '@/components/BotaoWhatsApp';
import { buscarPlanta } from '@/lib/api';
import {
  AMBIENTE,
  DIFICULDADE,
  LUMINOSIDADE,
  LUMINOSIDADE_DETALHE,
  REGA,
  altura,
  caminhoDaImagem,
  precoEmReal,
} from '@/lib/rotulos';

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props) {
  const { id } = await params;
  const planta = await buscarPlanta(id);

  if (!planta) return { title: 'Planta não encontrada | Florescer' };

  // Título e descrição próprios por planta: é o que aparece no resultado do
  // Google e na prévia quando alguém manda o link no WhatsApp.
  return {
    title: `${planta.name} | Florescer`,
    description: planta.description,
    openGraph: {
      title: planta.name,
      description: planta.description,
      images: [planta.imageUrl],
    },
  };
}

/** A página que decide a compra: tudo o que a pessoa perguntaria, junto. */
export default async function DetalheDaPlanta({ params }: Props) {
  const { id } = await params;
  const planta = await buscarPlanta(id);

  if (!planta) notFound();

  const tamanho = altura(planta.heightCm);

  return (
    <main className="mx-auto max-w-5xl px-4 py-8">
      <Link href="/" className="mb-6 inline-block text-sm text-emerald-700 hover:underline">
        &larr; Voltar para as plantas
      </Link>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="relative aspect-square overflow-hidden rounded-xl bg-stone-100">
          <Image
            src={caminhoDaImagem(planta.imageUrl)}
            alt={planta.name}
            fill
            sizes="(max-width: 768px) 100vw, 50vw"
            className="object-cover"
            priority
          />
        </div>

        <div className="flex flex-col">
          <h1 className="text-3xl font-bold text-stone-900">{planta.name}</h1>
          <p className="mt-1 text-sm text-stone-500">{planta.type}</p>

          <p className="mt-4 text-3xl font-bold text-stone-900">{precoEmReal(planta.price)}</p>
          {planta.includesPot !== null && (
            <p className="mt-1 text-sm text-stone-600">
              {planta.includesPot ? 'Vaso incluso' : 'Sem o vaso'}
            </p>
          )}

          <p className="mt-6 leading-relaxed text-stone-700">{planta.description}</p>

          <dl className="mt-6 grid grid-cols-2 gap-4">
            {tamanho && <Dado termo="Altura" valor={tamanho} />}
            {planta.light && (
              <Dado
                termo="Luz"
                valor={LUMINOSIDADE[planta.light]}
                detalhe={LUMINOSIDADE_DETALHE[planta.light]}
              />
            )}
            {planta.watering && <Dado termo="Rega" valor={REGA[planta.watering]} />}
            {planta.environment && <Dado termo="Ambiente" valor={AMBIENTE[planta.environment]} />}
            {planta.difficulty && <Dado termo="Cuidado" valor={DIFICULDADE[planta.difficulty]} />}
          </dl>

          {/* Segurança para animais ganha destaque próprio, e aparece nos dois
              sentidos. Quem tem gato precisa ver o aviso, não a ausência dele. */}
          {planta.petSafe !== null && (
            <p
              className={`mt-6 rounded-lg px-4 py-3 text-sm ${
                planta.petSafe
                  ? 'bg-emerald-50 text-emerald-800'
                  : 'bg-amber-50 text-amber-900'
              }`}
            >
              {planta.petSafe
                ? 'Convive bem com cães e gatos.'
                : 'Tóxica se ingerida. Mantenha longe de crianças e animais.'}
            </p>
          )}

          {planta.careRequirements && (
            <div className="mt-6">
              <h2 className="mb-1 text-sm font-medium text-stone-500">Como cuidar</h2>
              <p className="leading-relaxed text-stone-700">{planta.careRequirements}</p>
            </div>
          )}

          <div className="mt-8">
            <BotaoWhatsApp planta={planta} />
          </div>
        </div>
      </div>
    </main>
  );
}

function Dado({ termo, valor, detalhe }: { termo: string; valor: string; detalhe?: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-stone-500">{termo}</dt>
      <dd className="font-medium text-stone-900">{valor}</dd>
      {detalhe && <dd className="text-xs text-stone-500">{detalhe}</dd>}
    </div>
  );
}
