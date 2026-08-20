import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/**
 * O que estes testes protegem
 *
 * Duas falhas opostas, e as duas silenciosas:
 *
 * - bloquear a vitrine por engano faz a loja sumir da busca, e ninguém percebe
 *   até notar que não chega visita;
 * - liberar `/admin` gasta o rastreamento em páginas que exigem sessão e faz
 *   "florescer login" virar resultado de busca.
 *
 * Nada disto é proteção: quem impede o acesso ao painel é o backend, que recusa
 * requisição sem token de ADMIN. Um arquivo de texto não impede ninguém.
 */

const SITE = 'https://florescerplantas.com.br';

async function gerar() {
  vi.resetModules();
  const { default: robots } = await import('./robots');
  return robots();
}

beforeEach(() => {
  vi.stubEnv('SITE_URL', SITE);
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('o que os buscadores podem ver', () => {
  it('libera a vitrine, que é justamente o que precisa ser achado', async () => {
    const r = await gerar();
    const regra = Array.isArray(r.rules) ? r.rules[0] : r.rules;

    expect(regra.userAgent).toBe('*');
    expect(regra.allow).toBe('/');
  });

  it('bloqueia o painel e o login', async () => {
    const r = await gerar();
    const regra = Array.isArray(r.rules) ? r.rules[0] : r.rules;
    const bloqueados = [regra.disallow].flat();

    expect(bloqueados).toContain('/admin');
    expect(bloqueados).toContain('/login');
  });

  it('não bloqueia a vitrine nem as páginas de planta', async () => {
    const r = await gerar();
    const regra = Array.isArray(r.rules) ? r.rules[0] : r.rules;
    const bloqueados = [regra.disallow].flat();

    expect(bloqueados).not.toContain('/');
    expect(bloqueados.some((c) => String(c).startsWith('/planta'))).toBe(false);
  });
});

describe('sitemap anunciado', () => {
  it('aponta para o sitemap no endereço público', async () => {
    const r = await gerar();
    expect(r.sitemap).toBe(`${SITE}/sitemap.xml`);
  });

  it('acompanha o endereço configurado, sem barra dupla', async () => {
    vi.stubEnv('SITE_URL', 'https://outra-loja.com.br/');
    const r = await gerar();
    expect(r.sitemap).toBe('https://outra-loja.com.br/sitemap.xml');
  });
});
