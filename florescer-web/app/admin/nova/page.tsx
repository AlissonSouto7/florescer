import Link from 'next/link';

import { FormularioPlanta } from '@/components/FormularioPlanta';

export const metadata = { title: 'Cadastrar planta | Florescer' };

export default function NovaPlanta() {
  return (
    <main className="mx-auto max-w-2xl px-4 py-8">
      <Link href="/admin" className="mb-4 inline-block text-sm text-emerald-700 hover:underline">
        &larr; Voltar
      </Link>

      <h1 className="mb-6 text-2xl font-bold text-stone-900">Cadastrar planta</h1>

      <FormularioPlanta />
    </main>
  );
}
