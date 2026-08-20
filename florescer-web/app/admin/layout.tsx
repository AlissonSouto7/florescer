import type { Metadata } from 'next';

/**
 * Mantém o painel fora dos buscadores.
 *
 * O `robots.txt` já pede isso, mas ele é um pedido sobre o que rastrear, e não
 * impede o endereço de ser indexado quando alguém o publica em outro lugar. A
 * meta tag `noindex` é a instrução que vale mesmo assim.
 *
 * Nada disso é proteção. Quem protege é o backend, que recusa qualquer
 * requisição sem token de ADMIN. Isto é higiene: "florescer login" não é
 * resultado útil para ninguém, e cada visita de robô a uma página que exige
 * sessão gasta o rastreamento que deveria ir para as plantas.
 *
 * Existe como layout porque as páginas do painel são componentes de cliente, e
 * componente de cliente não pode exportar `metadata`.
 */
export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function LayoutDoPainel({ children }: { children: React.ReactNode }) {
  return children;
}
