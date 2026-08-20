import { afterEach, describe, expect, it, vi } from 'vitest';

import { enderecoDoSite } from './site';

/**
 * O que estes testes protegem
 *
 * Este endereço vira o que o Google indexa e o que aparece na prévia do link
 * compartilhado no WhatsApp. Errar aqui não quebra tela nenhuma: a loja
 * continua funcionando, e é o buscador que passa a apontar para endereços que
 * não existem. Ninguém percebe até alguém clicar num resultado e não chegar.
 *
 * A barra no fim é o caso chato: `https://loja.com/` mais `/planta/1` vira
 * `https://loja.com//planta/1`, que responde, mas é uma URL diferente para o
 * buscador, e o conteúdo passa a contar como duplicado.
 */

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('enderecoDoSite', () => {
  it('usa o que está configurado', () => {
    vi.stubEnv('SITE_URL', 'https://florescerplantas.com.br');
    expect(enderecoDoSite()).toBe('https://florescerplantas.com.br');
  });

  it('remove a barra do fim, para não gerar URL com barra dupla', () => {
    vi.stubEnv('SITE_URL', 'https://florescerplantas.com.br/');
    expect(enderecoDoSite()).toBe('https://florescerplantas.com.br');
  });

  it('remove mais de uma barra, se alguém digitar demais', () => {
    vi.stubEnv('SITE_URL', 'https://florescerplantas.com.br///');
    expect(enderecoDoSite()).toBe('https://florescerplantas.com.br');
  });

  it('ignora espaço em volta, que passa despercebido num arquivo .env', () => {
    vi.stubEnv('SITE_URL', '  https://florescerplantas.com.br  ');
    expect(enderecoDoSite()).toBe('https://florescerplantas.com.br');
  });

  it('cai no localhost quando não há configuração', () => {
    vi.stubEnv('SITE_URL', '');
    expect(enderecoDoSite()).toBe('http://localhost:3000');
  });

  it('devolve algo que o construtor de URL aceita', () => {
    // O layout faz `new URL(enderecoDoSite())` no metadataBase. Um valor que o
    // parser rejeita derruba a renderização de toda página.
    for (const valor of ['https://loja.com.br/', '  http://localhost:3000 ', '']) {
      vi.stubEnv('SITE_URL', valor);
      expect(() => new URL(enderecoDoSite())).not.toThrow();
    }
  });

  it('preserva a porta e o caminho quando existem', () => {
    vi.stubEnv('SITE_URL', 'http://192.168.0.10:8080');
    expect(enderecoDoSite()).toBe('http://192.168.0.10:8080');
  });
});
