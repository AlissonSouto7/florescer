import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { LOJA_VAZIA, buscarDadosDaLoja, linkDoInstagram, salvarDadosDaLoja } from './loja';

/**
 * O que estes testes protegem
 *
 * Estes dados aparecem no rodapé de toda página e decidem para onde o botão de
 * comprar leva. Dois riscos, e os dois silenciosos:
 *
 * - **a API fora do ar derrubando a loja inteira**: sem tratamento, uma falha
 *   aqui viraria tela de erro em vez de um rodapé mais curto. Vitrine sem
 *   rodapé vende; tela de erro, não;
 * - **o link do Instagram montado errado**: o backend guarda o perfil sem o
 *   arroba, e quem monta o link precisa saber disso. Um `@` a mais leva a uma
 *   página que não existe, e ninguém testa link de rodapé.
 */

let fetchFalso: ReturnType<typeof vi.fn>;

const RESPOSTA = {
  whatsappNumber: '5573998149668',
  deliveryCity: 'Itabuna, BA',
  instagramHandle: 'florescer.plantas',
  openingHours: 'Segunda a sábado, das 8h às 18h',
};

beforeEach(() => {
  fetchFalso = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => RESPOSTA,
  } as Response);
  vi.stubGlobal('fetch', fetchFalso);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('buscarDadosDaLoja', () => {
  it('devolve o que a API respondeu', async () => {
    await expect(buscarDadosDaLoja()).resolves.toEqual(RESPOSTA);
  });

  it('busca pelo domínio do site, sem host', async () => {
    // Mesma regra do catálogo: o navegador fala só com este domínio, e as APIs
    // ficam fora da internet.
    await buscarDadosDaLoja();

    const endereco = fetchFalso.mock.calls[0][0] as string;
    expect(endereco).not.toMatch(/^https?:\/\//);
    expect(endereco).toContain('settings');
  });

  it('não guarda em cache, senão ela salva e continua vendo o valor antigo', async () => {
    // Este caso nasceu de uma mutação que passou verde: trocar `no-store` por
    // `revalidate: 60` não quebrava teste nenhum, e era exatamente o defeito
    // que já tinha acontecido uma vez. Ela salvava o número, abria a loja, via
    // o antigo e concluía que não tinha salvado.
    await buscarDadosDaLoja();

    const opcoes = fetchFalso.mock.calls[0][1] as RequestInit;
    expect(opcoes.cache).toBe('no-store');
    expect(opcoes).not.toHaveProperty('next');
  });

  it('devolve a loja vazia quando a API responde erro', async () => {
    // A alternativa seria propagar a falha, e aí uma API instável derrubaria a
    // vitrine inteira por causa do rodapé.
    fetchFalso.mockResolvedValue({ ok: false, status: 500 } as Response);
    await expect(buscarDadosDaLoja()).resolves.toEqual(LOJA_VAZIA);
  });

  it('devolve a loja vazia quando a rede falha', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'));
    await expect(buscarDadosDaLoja()).resolves.toEqual(LOJA_VAZIA);
  });

  it('a loja vazia tem todos os campos nulos, e não indefinidos', async () => {
    // Nulo é o que o rodapé sabe tratar: campo ausente some da tela. Um
    // `undefined` passaria por qualquer verificação frouxa e viraria "undefined"
    // escrito no rodapé.
    for (const valor of Object.values(LOJA_VAZIA)) {
      expect(valor).toBeNull();
    }
  });
});

describe('salvarDadosDaLoja', () => {
  it('manda PUT com o token', async () => {
    await salvarDadosDaLoja(RESPOSTA, 'token.de.admin');

    const opcoes = fetchFalso.mock.calls[0][1] as RequestInit;
    expect(opcoes.method).toBe('PUT');
    expect((opcoes.headers as Record<string, string>).Authorization).toBe('Bearer token.de.admin');
  });

  it('manda os dados como JSON', async () => {
    await salvarDadosDaLoja(RESPOSTA, 'tok');

    const opcoes = fetchFalso.mock.calls[0][1] as RequestInit;
    expect((opcoes.headers as Record<string, string>)['Content-Type']).toBe('application/json');
    expect(JSON.parse(opcoes.body as string)).toEqual(RESPOSTA);
  });

  it('devolve a resposta crua, para a tela distinguir os casos', async () => {
    // 400 mostra o aviso no campo, 401 manda para o login, e o resto é falha de
    // sistema. Converter aqui apagaria essa diferença.
    const resposta = await salvarDadosDaLoja(RESPOSTA, 'tok');
    expect(resposta.status).toBe(200);
  });
});

describe('linkDoInstagram', () => {
  it('monta o endereço do perfil', () => {
    expect(linkDoInstagram('florescer.plantas')).toBe('https://instagram.com/florescer.plantas');
  });

  it('não devolve link quando não há perfil', () => {
    // Sem isso o rodapé mostraria um link para instagram.com/null.
    expect(linkDoInstagram(null)).toBeNull();
    expect(linkDoInstagram('')).toBeNull();
  });

  it('não repete o arroba, que o backend já tirou', () => {
    const link = linkDoInstagram('florescer.plantas')!;
    expect(link).not.toContain('@');
  });
});
