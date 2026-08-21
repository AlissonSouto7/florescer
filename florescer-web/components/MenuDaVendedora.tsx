'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

import { ehAdmin, esquecerToken, expirado, lerToken } from '@/lib/sessao';

/**
 * O acesso ao painel, visível só para quem já entrou.
 *
 * Antes o link "Área da vendedora" ficava no cabeçalho o tempo todo. Para quem
 * chega para comprar uma planta, ele não significa nada: é um convite a clicar
 * numa tela de login que a pessoa não tem como usar, e faz a loja parecer um
 * sistema interno em vez de uma vitrine.
 *
 * Isto não é segurança, e não deve ser confundido com ela. Quem protege o
 * painel é o backend, que recusa qualquer requisição sem token de ADMIN.
 * Esconder o link é sobre não poluir a experiência de quem veio comprar.
 *
 * A sessão vive no navegador, então nada disso pode ser decidido no servidor.
 * A verificação roda depois da montagem: até lá, o espaço fica reservado mas
 * vazio, e é por isso que o menu não "pula" quando a página termina de carregar.
 */
export function MenuDaVendedora() {
  const router = useRouter();
  const caminho = usePathname();
  const [logada, setLogada] = useState<boolean | null>(null);

  /**
   * A rota entra como dependência, e isso não é detalhe.
   *
   * O layout não é remontado ao navegar: o Next troca só o conteúdo. Com a lista
   * de dependências vazia, a verificação rodava uma vez, na primeira página
   * aberta, e nunca mais. Quem entrasse pelo login chegava ao painel com o
   * cabeçalho ainda dizendo que não havia sessão, e só um F5 corrigia.
   *
   * Encontrado percorrendo a loja no navegador. Nenhum teste de componente
   * pegaria: isoladamente o componente funciona, e o defeito só existe na
   * costura entre login, navegação e layout.
   */
  useEffect(() => {
    const token = lerToken();
    setLogada(Boolean(token) && !expirado(token!) && ehAdmin(token!));
  }, [caminho]);

  // `null` é o estado antes da verificação, e não "não está logada": mostrar
  // qualquer coisa aqui faria o conteúdo trocar na frente da pessoa.
  if (logada === null) return <div className="h-9" aria-hidden="true" />;

  if (!logada) return null;

  function sair() {
    esquecerToken();
    setLogada(false);
    router.push('/');
    router.refresh();
  }

  return (
    <div className="flex items-center gap-1">
      <Link
        href="/admin"
        className="rounded-lg px-3 py-2 text-sm font-medium text-stone-700 transition
                   hover:bg-stone-100 hover:text-emerald-800"
      >
        Minhas plantas
      </Link>

      <button
        type="button"
        onClick={sair}
        className="rounded-lg px-3 py-2 text-sm text-stone-500 transition
                   hover:bg-stone-100 hover:text-stone-800"
      >
        Sair
      </button>
    </div>
  );
}
