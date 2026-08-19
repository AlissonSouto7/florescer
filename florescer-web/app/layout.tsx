import type { Metadata } from 'next';
import Link from 'next/link';

import './globals.css';

export const metadata: Metadata = {
  title: 'Florescer',
  description: 'Plantas com foto, tamanho, cuidados e preço.',
};

export default function RootLayout({ children }: LayoutProps<'/'>) {
  // lang="pt-BR" não é detalhe: o leitor de tela usa isso para escolher a
  // pronúncia, e o navegador para oferecer tradução.
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="flex min-h-full flex-col bg-stone-50 text-stone-900">
        <Cabecalho />
        <div className="flex-1">{children}</div>
        <Rodape />
      </body>
    </html>
  );
}

function Cabecalho() {
  return (
    <header className="border-b border-stone-200 bg-white">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
        <Link href="/" className="text-xl font-bold text-emerald-800">
          Florescer
        </Link>
        <Link
          href="/admin"
          className="text-sm text-stone-500 transition hover:text-emerald-700"
        >
          Área da vendedora
        </Link>
      </nav>
    </header>
  );
}

function Rodape() {
  return (
    <footer className="border-t border-stone-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 py-6 text-sm text-stone-500">
        Florescer &middot; plantas cultivadas em casa
      </div>
    </footer>
  );
}
