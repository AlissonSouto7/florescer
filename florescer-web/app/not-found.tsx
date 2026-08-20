import Link from 'next/link';

/**
 * Quando o endereço não existe.
 *
 * Sem esta página, quem segue um link antigo cai na tela padrão do Next: em
 * inglês, sem nada da loja e sem caminho de volta. É o oposto do que se quer
 * para alguém que já estava a um clique de comprar.
 *
 * Vale mais do que parece: a maior parte dos 404 aqui vem de link de planta
 * vendida, compartilhado no WhatsApp semanas antes. Essa pessoa quer plantas,
 * e o único trabalho desta página é levá-la de volta à vitrine.
 */
export default function NaoEncontrada() {
  return (
    <main className="mx-auto max-w-md px-4 py-24 text-center">
      <p className="text-5xl" aria-hidden="true">
        🌱
      </p>

      <h1 className="mt-4 text-2xl font-bold text-stone-900">Esta página não existe</h1>

      <p className="mt-2 text-stone-600">
        O endereço pode ter mudado, ou a planta que você procurava já foi vendida.
      </p>

      <Link
        href="/"
        className="mt-8 inline-block rounded-lg bg-emerald-600 px-6 py-3 font-medium text-white
                   transition hover:bg-emerald-700 focus:outline-none focus:ring-2
                   focus:ring-emerald-500 focus:ring-offset-2"
      >
        Ver as plantas disponíveis
      </Link>
    </main>
  );
}
