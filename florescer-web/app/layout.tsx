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

/**
 * O rodapé.
 *
 * Fundo escuro, e não a mesma folha branca do resto: é ele que fecha a página.
 * Sem essa mudança de tom, o conteúdo simplesmente termina, e o site parece
 * cortado. O verde profundo também é o único lugar da loja onde a cor da marca
 * aparece por inteiro, já que o miolo é claro de propósito, para a foto da
 * planta ser a única cor forte da tela.
 *
 * **Nenhum link para a área da vendedora**, em lugar nenhum do site. Quem
 * cadastra é uma pessoa só, que salva o endereço nos favoritos. Para todo o
 * resto do mundo, aquele caminho não existe, e anunciá-lo só faria a loja
 * parecer um sistema com uma porta de serviço à vista.
 */
function Rodape() {
  const ano = new Date().getFullYear();

  return (
    <footer className="mt-20 bg-emerald-950 text-emerald-50">
      <div className="mx-auto max-w-7xl px-6 py-14">
        <div className="grid gap-10 md:grid-cols-12">
          <div className="md:col-span-5">
            <div className="flex items-center gap-2">
              <svg viewBox="0 0 24 24" className="h-7 w-7 text-emerald-300" fill="currentColor" aria-hidden="true">
                <path d="M12 21c-.4-3.6.6-6.4 3-8.4 2.4-2 5.4-2.6 9-1.6-.4 3.6-1.8 6.2-4.2 7.8-2.4 1.6-5.1 2-7.8 2.2Z" opacity=".55" />
                <path d="M11 21C7.9 20.7 5.5 19.4 4 17 2.4 14.6 1.9 11.3 2.4 7c3.6.6 6.3 2 8.1 4.2 1.8 2.2 2.6 5.4 2.5 9.8H11Z" />
              </svg>
              <span className="text-xl font-bold tracking-tight">Florescer</span>
            </div>

            <p className="mt-4 max-w-sm leading-relaxed text-emerald-100/80">
              Plantas cultivadas em casa, escolhidas uma a uma. Cada uma vem com o
              tamanho, a luz que aguenta e os cuidados que pede, para você acertar
              antes de levar.
            </p>
          </div>

          <nav className="md:col-span-3" aria-labelledby="rodape-loja">
            <h2 id="rodape-loja" className="text-sm font-semibold uppercase tracking-wider text-emerald-300">
              Escolha por
            </h2>
            <ul className="mt-4 space-y-3 text-emerald-100/80">
              <li><LinkDoRodape href="/">Todas as plantas</LinkDoRodape></li>
              <li><LinkDoRodape href="/?petSafe=true">Seguras para pets</LinkDoRodape></li>
              <li><LinkDoRodape href="/?difficulty=FACIL">Fáceis de cuidar</LinkDoRodape></li>
              <li><LinkDoRodape href="/?environment=INTERNO">Para dentro de casa</LinkDoRodape></li>
            </ul>
          </nav>

          <div className="md:col-span-4">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-emerald-300">
              Como funciona
            </h2>

            <ol className="mt-4 space-y-3 text-emerald-100/80">
              <PassoDaCompra numero={1}>Escolha a planta pela foto e pelos cuidados.</PassoDaCompra>
              <PassoDaCompra numero={2}>Clique em comprar.</PassoDaCompra>
              <PassoDaCompra numero={3}>A conversa segue no WhatsApp, com o nome e o preço já na mensagem.</PassoDaCompra>
            </ol>
          </div>
        </div>

        <div className="mt-12 border-t border-emerald-800/60 pt-6 text-sm text-emerald-200/70">
          <p>&copy; {ano} Florescer. Plantas cultivadas e entregues com cuidado.</p>
        </div>
      </div>
    </footer>
  );
}

function LinkDoRodape({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      className="transition hover:text-white focus:outline-none focus-visible:underline focus-visible:decoration-emerald-300"
    >
      {children}
    </Link>
  );
}

function PassoDaCompra({ numero, children }: { numero: number; children: React.ReactNode }) {
  return (
    <li className="flex gap-3">
      <span
        aria-hidden="true"
        className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full
                   bg-emerald-800 text-xs font-semibold text-emerald-100"
      >
        {numero}
      </span>
      <span className="leading-relaxed">{children}</span>
    </li>
  );
}
