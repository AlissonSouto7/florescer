import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { alterarPlanta, buscarPlanta, entrar, errosDe, excluirPlanta, listarPlantas, salvarPlanta } from './api';

/**
 * O que estes testes protegem
 *
 * A montagem da query é onde o filtro deixa de funcionar sem avisar. Se um
 * parâmetro parar de ser enviado, a vitrine responde 200 e mostra plantas: só
 * que as erradas. Quem filtrou por "segura para cães e gatos" recebe planta
 * tóxica, e nada na tela indica que o filtro foi ignorado.
 *
 * O caso do 404 no detalhe é o outro: precisa virar "planta não encontrada", e
 * não uma tela de erro, porque link antigo compartilhado é situação normal.
 */

function respostaCom(corpo: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => corpo,
  } as Response;
}

/** O endereço cru que o código passou ao fetch, do jeito que ele passou. */
function enderecoChamado(mock: ReturnType<typeof vi.fn>): string {
  return mock.mock.calls[0][0] as string;
}

/**
 * A URL que o código realmente pediu, resolvida contra um domínio qualquer.
 *
 * A base existe só para o parser: no navegador as chamadas são caminhos
 * relativos, e `new URL` sem base rejeita caminho relativo. Qual é o domínio não
 * importa aqui, e é justamente esse o ponto (ver o caso "mesma origem").
 */
function urlChamada(mock: ReturnType<typeof vi.fn>): URL {
  return new URL(enderecoChamado(mock), 'http://loja.local');
}

let fetchFalso: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchFalso = vi.fn().mockResolvedValue(respostaCom({ content: [], totalElements: 0, totalPages: 0, number: 0 }));
  vi.stubGlobal('fetch', fetchFalso);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('tudo sai pela mesma origem', () => {
  /**
   * Este é o caso que protege a decisão de arquitetura.
   *
   * O navegador fala só com o domínio do site, e o servidor do Next repassa para
   * as APIs pela rede interna. Isso é o que permite manter `auth-service` e
   * `product-service` fora da internet, com uma porta exposta em vez de três.
   *
   * Se alguém voltar a montar a URL com um host (uma `NEXT_PUBLIC_API_URL`
   * qualquer), nada quebra na tela em desenvolvimento: as portas estão abertas
   * na máquina de quem programa. O estrago aparece em produção, onde as APIs não
   * estão publicadas, e a essa altura já foi para o ar.
   */
  const chamadas: Array<[string, () => Promise<unknown>]> = [
    ['listar', () => listarPlantas({ light: 'SOMBRA' })],
    ['detalhe', () => buscarPlanta('id-1')],
    ['login', () => entrar('a@b.com', 'senha').catch(() => null)],
    ['cadastrar', () => salvarPlanta({}, null, 'tok')],
    ['editar', () => alterarPlanta('id-1', {}, null, 'tok')],
    ['excluir', () => excluirPlanta('id-1', 'tok')],
  ];

  it.each(chamadas)('%s usa caminho relativo, sem host', async (_nome, chamar) => {
    fetchFalso.mockResolvedValue(respostaCom({ accessToken: 't' }));
    await chamar();

    const endereco = enderecoChamado(fetchFalso);
    expect(endereco.startsWith('/')).toBe(true);
    expect(endereco).not.toMatch(/^https?:\/\//);
    expect(endereco).not.toContain('localhost');
    expect(endereco).not.toContain('product-service');
    expect(endereco).not.toContain('auth-service');
  });
});

describe('listarPlantas: o que vai na query', () => {
  it('sempre envia página e tamanho, mesmo sem filtro', async () => {
    await listarPlantas();
    const url = urlChamada(fetchFalso);
    expect(url.pathname).toBe('/api/product');
    expect(url.searchParams.get('page')).toBe('0');
    expect(url.searchParams.get('size')).toBe('12');
  });

  it('envia cada filtro escolhido', async () => {
    await listarPlantas({
      light: 'MEIA_SOMBRA',
      environment: 'INTERNO',
      difficulty: 'FACIL',
      petSafe: true,
      onlyAvailable: true,
      maxPrice: 50,
    });

    const p = urlChamada(fetchFalso).searchParams;
    expect(p.get('light')).toBe('MEIA_SOMBRA');
    expect(p.get('environment')).toBe('INTERNO');
    expect(p.get('difficulty')).toBe('FACIL');
    expect(p.get('petSafe')).toBe('true');
    expect(p.get('onlyAvailable')).toBe('true');
    expect(p.get('maxPrice')).toBe('50');
  });

  it('omite o que não foi escolhido, em vez de mandar vazio', async () => {
    // Parâmetro vazio faria o backend filtrar por nada e deixaria a URL da
    // página ilegível para quem quiser compartilhar o link.
    await listarPlantas({ light: 'SOMBRA' });

    const p = urlChamada(fetchFalso).searchParams;
    expect(p.has('environment')).toBe(false);
    expect(p.has('difficulty')).toBe(false);
    expect(p.has('petSafe')).toBe(false);
    expect(p.has('maxPrice')).toBe(false);
  });

  it('não envia petSafe quando a pessoa desmarcou', async () => {
    // petSafe=false significa "tanto faz", não "quero as tóxicas". Mandar false
    // devolveria só as tóxicas, o oposto do esperado.
    await listarPlantas({ petSafe: false, onlyAvailable: false });

    const p = urlChamada(fetchFalso).searchParams;
    expect(p.has('petSafe')).toBe(false);
    expect(p.has('onlyAvailable')).toBe(false);
  });

  it('respeita a página pedida', async () => {
    await listarPlantas({ page: 2, size: 24 });

    const p = urlChamada(fetchFalso).searchParams;
    expect(p.get('page')).toBe('2');
    expect(p.get('size')).toBe('24');
  });

  it('avisa quando a vitrine não responde, em vez de devolver lista vazia', async () => {
    // Lista vazia silenciosa pareceria "nenhuma planta cadastrada", e a
    // vendedora acharia que perdeu o catálogo.
    fetchFalso.mockResolvedValue(respostaCom({}, 500));
    await expect(listarPlantas()).rejects.toThrow(/500/);
  });
});

describe('buscarPlanta', () => {
  it('devolve nulo no 404, para a página mostrar "não encontrada"', async () => {
    fetchFalso.mockResolvedValue(respostaCom({}, 404));
    await expect(buscarPlanta('sumiu')).resolves.toBeNull();
  });

  it('lança quando o serviço está fora, porque aí não é link velho', async () => {
    fetchFalso.mockResolvedValue(respostaCom({}, 503));
    await expect(buscarPlanta('x')).rejects.toThrow(/503/);
  });
});

describe('entrar', () => {
  it('devolve o accessToken que a API manda', async () => {
    fetchFalso.mockResolvedValue(respostaCom({ accessToken: 'token.de.verdade' }));
    await expect(entrar('a@b.com', 'senha')).resolves.toBe('token.de.verdade');
  });

  it('manda e-mail e senha como JSON no corpo', async () => {
    fetchFalso.mockResolvedValue(respostaCom({ accessToken: 't' }));
    await entrar('a@b.com', 'senha');

    const opcoes = fetchFalso.mock.calls[0][1] as RequestInit;
    expect(opcoes.method).toBe('POST');
    expect(JSON.parse(opcoes.body as string)).toEqual({ email: 'a@b.com', password: 'senha' });
  });

  it('não revela se a conta existe', async () => {
    // A API responde igual para conta inexistente e senha errada, de propósito.
    // A interface não pode ser mais específica que ela.
    fetchFalso.mockResolvedValue(respostaCom({}, 401));
    await expect(entrar('a@b.com', 'errada')).rejects.toThrow('E-mail ou senha incorretos.');

    fetchFalso.mockResolvedValue(respostaCom({}, 404));
    await expect(entrar('naoexiste@b.com', 'x')).rejects.toThrow('E-mail ou senha incorretos.');
  });

  it('não chama de "senha errada" o que é falha do sistema', async () => {
    /**
     * Isto custou uma hora de diagnóstico numa mudança de infraestrutura.
     *
     * Uma configuração errada de CORS fazia o login responder 403 "Invalid CORS
     * request". A tela dizia "E-mail ou senha incorretos", então a conclusão
     * óbvia era senha errada, e a senha estava certa. Para a vendedora seria
     * pior: ela tentaria de novo, trocaria a senha, e continuaria sem entrar.
     *
     * A mensagem genérica existe para não revelar se a conta existe, e isso vale
     * só para 401 e 404. Qualquer outro status é problema do sistema, e dizer
     * isso não entrega informação nenhuma sobre a conta.
     */
    for (const status of [403, 429, 500, 502, 503]) {
      fetchFalso.mockResolvedValue(respostaCom({}, status));
      await expect(entrar('a@b.com', 'certa')).rejects.toThrow(/não foi possível entrar/i);
    }
  });

  it('menciona o código do erro, para quem for investigar', async () => {
    fetchFalso.mockResolvedValue(respostaCom({}, 403));
    await expect(entrar('a@b.com', 'certa')).rejects.toThrow(/403/);
  });
});

describe('salvar, alterar e excluir', () => {
  const DADOS = { name: 'Samambaia', price: 49.9 };
  const FOTO = new File(['bytes'], 'planta.png', { type: 'image/png' });

  /** O FormData que foi mandado na última chamada. */
  const corpoEnviado = () => (fetchFalso.mock.calls[0][1] as RequestInit).body as FormData;

  it('manda os dados como parte "product" em JSON, que é o que o backend espera', async () => {
    // O backend lê `@RequestPart("product")` tipado. Campos soltos no FormData,
    // ou um Blob sem o tipo application/json, viram 400 sem explicação útil.
    await salvarPlanta(DADOS, FOTO, 'tok');

    const parte = corpoEnviado().get('product') as Blob;
    expect(parte).toBeInstanceOf(Blob);
    expect(parte.type).toBe('application/json');
    expect(JSON.parse(await parte.text())).toEqual(DADOS);
  });

  it('manda a foto na parte "image"', async () => {
    await salvarPlanta(DADOS, FOTO, 'tok');
    expect(corpoEnviado().get('image')).toBe(FOTO);
  });

  it('omite a parte "image" quando não há foto nova', async () => {
    // No PATCH, mandar image vazio faria o backend trocar a foto por nada.
    await alterarPlanta('id-1', DADOS, null, 'tok');
    expect(corpoEnviado().has('image')).toBe(false);
  });

  it('cadastra com POST e edita com PATCH no id certo', async () => {
    await salvarPlanta(DADOS, FOTO, 'tok');
    expect((fetchFalso.mock.calls[0][1] as RequestInit).method).toBe('POST');
    expect(urlChamada(fetchFalso).pathname).toBe('/api/product');

    fetchFalso.mockClear();
    await alterarPlanta('id-1', DADOS, null, 'tok');
    expect((fetchFalso.mock.calls[0][1] as RequestInit).method).toBe('PATCH');
    expect(urlChamada(fetchFalso).pathname).toBe('/api/product/id-1');
  });

  it('exclui pelo id, com DELETE', async () => {
    await excluirPlanta('id-1', 'tok');
    expect((fetchFalso.mock.calls[0][1] as RequestInit).method).toBe('DELETE');
    expect(urlChamada(fetchFalso).pathname).toBe('/api/product/id-1');
  });

  it('leva o token nas três, senão a API responde 401', async () => {
    for (const chamada of [
      () => salvarPlanta(DADOS, FOTO, 'tok'),
      () => alterarPlanta('id-1', DADOS, null, 'tok'),
      () => excluirPlanta('id-1', 'tok'),
    ]) {
      fetchFalso.mockClear();
      await chamada();
      const cabecalhos = (fetchFalso.mock.calls[0][1] as RequestInit).headers as Record<string, string>;
      expect(cabecalhos.Authorization).toBe('Bearer tok');
    }
  });

  it('não define Content-Type na mão: o navegador precisa gerar o boundary', async () => {
    // Content-Type multipart sem boundary faz o servidor não conseguir separar
    // as partes, e a requisição falha com um erro que não aponta para a causa.
    await salvarPlanta(DADOS, FOTO, 'tok');
    const cabecalhos = (fetchFalso.mock.calls[0][1] as RequestInit).headers as Record<string, string>;
    expect(cabecalhos['Content-Type']).toBeUndefined();
  });
});

describe('errosDe', () => {
  it('separa a mensagem por campo, para exibir ao lado do input certo', async () => {
    const resposta = respostaCom({ error: 'Erro de validação', details: { price: 'deve ser positivo' } }, 400);
    await expect(errosDe(resposta)).resolves.toEqual({ price: 'deve ser positivo' });
  });

  it('usa a mensagem geral quando o erro não é de campo', async () => {
    const resposta = respostaCom({ error: 'Imagem inválida' }, 400);
    await expect(errosDe(resposta)).resolves.toEqual({ _geral: 'Imagem inválida' });
  });

  it('sobrevive a resposta que não é JSON', async () => {
    // 413 do container costuma vir como HTML, e a tela não pode ficar em branco.
    const resposta = {
      ok: false,
      status: 413,
      json: async () => {
        throw new SyntaxError('Unexpected token <');
      },
    } as unknown as Response;
    await expect(errosDe(resposta)).resolves.toEqual({ _geral: 'Erro 413' });
  });
});
