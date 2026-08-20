import type { MetadataRoute } from 'next';

import { listarPlantas, type Planta } from '@/lib/api';
import { enderecoDoSite } from '@/lib/site';

/**
 * A lista de páginas que o Google deve conhecer.
 *
 * Sem ela, o buscador só encontra uma planta se chegar até o link dela partindo
 * da vitrine, e planta que caiu para a segunda página pode nunca ser alcançada.
 * Com ela, a lista inteira é entregue de uma vez.
 *
 * Só entram plantas disponíveis: indexar o que não pode ser vendido faz alguém
 * chegar pelo Google numa página que diz "indisponível", e sair da loja.
 */

/**
 * O maior `size` que a API aceita, definido em `PageableFactory.MAX_PAGE_SIZE`.
 *
 * Pedir mais responde `400`, e não uma lista truncada. A recusa é deliberada, e
 * é boa: um sitemap com metade das plantas passaria despercebido, enquanto o
 * erro aparece. Por isso aqui se pagina em vez de pedir tudo de uma vez.
 */
const POR_PAGINA = 50;

/** Teto de segurança: 40 páginas são 2000 plantas, muito além desta loja. */
const MAXIMO_DE_PAGINAS = 40;

/**
 * Gerado a cada requisição, e nunca no build.
 *
 * Sem isto o Next monta o sitemap durante o `next build`, quando a API não está
 * de pé (no Docker ela nem existe ainda, porque a rede sobe depois). A chamada
 * falha, o `catch` devolve só a vitrine, e o arquivo fica **sem nenhuma planta**
 * até o primeiro revalidate. Medido: 1 URL logo após subir, 8 depois de expirar.
 *
 * O risco é o buscador pedir o sitemap justamente nessa janela e concluir que a
 * loja tem uma página só. Sitemap é pedido raramente, então gerar sob demanda
 * custa pouco, e o `revalidate: 30` de `listarPlantas` já evita bater no banco a
 * cada chamada.
 */
export const dynamic = 'force-dynamic';

async function todasAsPlantasDisponiveis(): Promise<Planta[]> {
  const plantas: Planta[] = [];

  for (let pagina = 0; pagina < MAXIMO_DE_PAGINAS; pagina++) {
    const resposta = await listarPlantas({ page: pagina, size: POR_PAGINA, onlyAvailable: true });
    plantas.push(...resposta.content);

    if (pagina >= resposta.totalPages - 1) break;
  }

  return plantas;
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const site = enderecoDoSite();

  const vitrine: MetadataRoute.Sitemap = [
    {
      url: site,
      // Muda toda vez que uma planta é cadastrada ou vendida.
      changeFrequency: 'daily',
      priority: 1,
    },
  ];

  try {
    const plantas = await todasAsPlantasDisponiveis();

    return [
      ...vitrine,
      ...plantas.map((planta) => ({
        url: `${site}/planta/${planta.id}`,
        changeFrequency: 'weekly' as const,
        priority: 0.8,
      })),
    ];
  } catch {
    // O catálogo fora do ar não pode derrubar o sitemap: um erro aqui faria o
    // Google receber 500 e voltar com menos frequência. Só a vitrine é pior que
    // a lista completa, e muito melhor que nada.
    return vitrine;
  }
}
