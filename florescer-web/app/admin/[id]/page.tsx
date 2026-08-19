import Link from 'next/link';
import { notFound } from 'next/navigation';

import { FormularioPlanta } from '@/components/FormularioPlanta';
import { buscarPlanta } from '@/lib/api';

export const metadata = { title: 'Editar planta | Florescer' };

export default async function EditarPlanta({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  // Carregado no servidor: o formulário já chega preenchido, em vez de piscar
  // vazio e preencher depois que o JavaScript busca.
  const planta = await buscarPlanta(id);
  if (!planta) notFound();

  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <Link href="/admin" className="mb-4 inline-block text-sm text-emerald-700 hover:underline">
        &larr; Voltar
      </Link>

      <h1 className="mb-1 text-2xl font-bold text-stone-900">Editar planta</h1>
      <p className="mb-6 text-sm text-stone-600">{planta.name}</p>

      <FormularioPlanta planta={planta} />
    </main>
  );
}
