'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';

import { entrar } from '@/lib/api';
import { ehAdmin, guardarToken } from '@/lib/sessao';

export default function Login() {
  const router = useRouter();
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function aoEnviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);

    const form = new FormData(evento.currentTarget);

    try {
      const token = await entrar(String(form.get('email')), String(form.get('senha')));

      if (!ehAdmin(token)) {
        // Conta comum autentica, mas não administra. Dizer isso é melhor que
        // deixar entrar e mostrar uma tela onde tudo dá erro de permissão.
        setErro('Esta conta não tem acesso à área da vendedora.');
        return;
      }

      guardarToken(token);
      router.push('/admin');
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível entrar.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="mx-auto max-w-sm px-4 py-16">
      <h1 className="text-2xl font-bold text-stone-900">Entrar</h1>
      <p className="mt-1 text-sm text-stone-600">Área de quem cadastra as plantas.</p>

      <form onSubmit={aoEnviar} className="mt-8 space-y-4">
        <div>
          <label htmlFor="email" className="mb-1 block text-sm font-medium text-stone-700">
            E-mail
          </label>
          <input
            id="email"
            name="email"
            type="email"
            required
            autoComplete="email"
            className="w-full rounded-lg border border-stone-300 px-3 py-2
                       focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>

        <div>
          <label htmlFor="senha" className="mb-1 block text-sm font-medium text-stone-700">
            Senha
          </label>
          <input
            id="senha"
            name="senha"
            type="password"
            required
            autoComplete="current-password"
            className="w-full rounded-lg border border-stone-300 px-3 py-2
                       focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
        </div>

        {erro && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {erro}
          </p>
        )}

        <button
          type="submit"
          disabled={enviando}
          // Desabilitar enquanto envia evita o duplo clique que dispara duas
          // tentativas e esbarra no rate limit do backend.
          className="w-full rounded-lg bg-emerald-600 px-4 py-2.5 font-medium text-white
                     transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-stone-300"
        >
          {enviando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </main>
  );
}
