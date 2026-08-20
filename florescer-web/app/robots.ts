import type { MetadataRoute } from 'next';

import { enderecoDoSite } from '@/lib/site';

/**
 * O que os buscadores podem visitar.
 *
 * A vitrine é aberta, e é justamente ela que precisa ser encontrada. O painel e
 * o login ficam de fora por dois motivos: aparecer numa busca por "florescer
 * login" não ajuda ninguém, e cada visita de robô a uma página que exige sessão
 * gasta o orçamento de rastreamento que deveria ir para as plantas.
 *
 * Isto não é proteção. Quem quiser entrar no painel não é impedido por um
 * arquivo de texto; quem impede é o backend, que recusa qualquer requisição sem
 * token de ADMIN.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/admin', '/admin/', '/login'],
    },
    sitemap: `${enderecoDoSite()}/sitemap.xml`,
  };
}
