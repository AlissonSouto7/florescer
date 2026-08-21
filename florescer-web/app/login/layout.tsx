import type { Metadata } from 'next';

/** Mesmo motivo do painel: ver `app/admin/layout.tsx`. */
export const metadata: Metadata = {
  title: 'Entrar',
  robots: { index: false, follow: false },
};

export default function LayoutDoLogin({ children }: { children: React.ReactNode }) {
  return children;
}
