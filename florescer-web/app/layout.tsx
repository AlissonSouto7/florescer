import type { Metadata } from 'next';
import Link from 'next/link';

import { MenuDaVendedora } from '@/components/MenuDaVendedora';
import { enderecoDoSite } from '@/lib/site';

import './globals.css';

export const metadata: Metadata = {
  // Resolve todo caminho relativo dos metadados (imagem de compartilhamento,
  // canonical) contra o endereço público. Sem isto o Next avisa no build e
  // monta as URLs contra localhost, então a prévia do link no WhatsApp fica sem
  // foto para todo mundo que não seja você.
  metadataBase: new URL(enderecoDoSite()),
  title: {
    default: 'Florescer | Plantas',
    // Cada página completa o título: "Samambaia | Florescer".
    template: '%s | Florescer',
  },
  description: 'Plantas com foto, tamanho, cuidados e preço. Fale direto pelo WhatsApp.',
  // O painel e o login não devem aparecer em busca. O robots.txt já pede isso,
  // e a meta tag é a instrução que vale mesmo quando alguém chega por um link.
  robots: { index: true, follow: true },
  openGraph: {
    type: 'website',
    locale: 'pt_BR',
    siteName: 'Florescer',
  },
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
    <header className="sticky top-0 z-20 border-b border-stone-200 bg-white/90 backdrop-blur">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <Link href="/" className="group flex items-center gap-2" aria-label="Florescer, página inicial">
          {/* Uma folha desenhada, e não emoji: emoji muda de forma em cada
              sistema, e a marca deixaria de ser a mesma no celular e no PC. */}
          <svg
            viewBox="0 0 24 24"
            className="h-7 w-7 text-emerald-700 transition group-hover:text-emerald-800"
            fill="currentColor"
            aria-hidden="true"
          >
            <path d="M12 21c-.4-3.6.6-6.4 3-8.4 2.4-2 5.4-2.6 9-1.6-.4 3.6-1.8 6.2-4.2 7.8-2.4 1.6-5.1 2-7.8 2.2Z" opacity=".55" />
            <path d="M11 21C7.9 20.7 5.5 19.4 4 17 2.4 14.6 1.9 11.3 2.4 7c3.6.6 6.3 2 8.1 4.2 1.8 2.2 2.6 5.4 2.5 9.8H11Z" />
          </svg>
          <span className="text-xl font-bold tracking-tight text-emerald-800">Florescer</span>
        </Link>

        {/* Só aparece para quem tem sessão: ver MenuDaVendedora. */}
        <MenuDaVendedora />
      </nav>
    </header>
  );
}

function Rodape() {
  const ano = new Date().getFullYear();

  return (
    <footer className="mt-16 border-t border-stone-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          <div className="sm:col-span-2">
            <p className="text-lg font-bold text-emerald-800">Florescer</p>
            <p className="mt-2 max-w-sm text-sm leading-relaxed text-stone-600">
              Plantas cultivadas em casa, escolhidas uma a uma. Cada planta vem com o
              tamanho, a luz que aguenta e os cuidados que precisa, para você acertar
              antes de levar.
            </p>
          </div>

          <div>
            <h2 className="text-sm font-semibold text-stone-900">A loja</h2>
            <ul className="mt-3 space-y-2 text-sm">
              <li>
                <Link href="/" className="text-stone-600 transition hover:text-emerald-700">
                  Todas as plantas
                </Link>
              </li>
              <li>
                <Link href="/?petSafe=true" className="text-stone-600 transition hover:text-emerald-700">
                  Seguras para pets
                </Link>
              </li>
              <li>
                <Link href="/?difficulty=FACIL" className="text-stone-600 transition hover:text-emerald-700">
                  Fáceis de cuidar
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h2 className="text-sm font-semibold text-stone-900">Como comprar</h2>
            <p className="mt-3 text-sm leading-relaxed text-stone-600">
              Escolha a planta, clique em comprar e a conversa segue pelo WhatsApp,
              já com o nome e o preço na mensagem.
            </p>
          </div>
        </div>

        <div className="mt-10 flex flex-col gap-2 border-t border-stone-200 pt-6 text-sm text-stone-500 sm:flex-row sm:items-center sm:justify-between">
          <p>&copy; {ano} Florescer. Todos os direitos reservados.</p>
          {/* O acesso ao painel fica aqui, discreto: quem vende sabe onde
              procurar, e quem veio comprar não tropeça nele. */}
          <Link href="/login" className="text-stone-400 transition hover:text-stone-700">
            Área da vendedora
          </Link>
        </div>
      </div>
    </footer>
  );
}
