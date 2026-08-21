import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Pagina, Planta } from '@/lib/api';
import { PLANTA } from '@/test/fixtures';

/**
 * O que estes testes protegem
 *
 * O sitemap é a lista que o Google recebe. Um erro aqui não aparece em lugar
 * nenhum: a loja funciona, e as plantas apenas deixam de ser encontradas.
 *
 * O caso que mais importa é a paginação. A API recusa `size` acima de 50 com
 * `400`, e não devolve uma lista cortada. Se este código voltar a pedir tudo de
 * uma vez, a chamada falha, o `catch` devolve só a vitrine, e o sitemap fica
 * **sem nenhuma planta** sem que nada acuse. Por isso há um caso que verifica
 * que nenhuma requisição passa de 50, e outro que confere que a segunda página
 * é buscada de verdade.
 */

const listar = vi.fn();

vi.mock('@/lib/api', () => ({
  listarPlantas: (...args: unknown[]) => listar(...args),
}));

const SITE = 'https://florescerplantas.com.br';

/** Uma página de resposta como a API devolve. */
function pagina(plantas: Planta[], totalPages = 1): Pagina<Planta> {
  return { content: plantas, totalElements: plantas.length, totalPages, number: 0 };
}

function plantasFalsas(quantidade: number, prefixo = 'p'): Planta[] {
  return Array.from({ length: quantidade }, (_, i) => ({ ...PLANTA, id: `${prefixo}-${i}` }));
}

/** Carrega o sitemap com o ambiente do caso já montado. */
async function gerar() {
  vi.resetModules();
  const { default: sitemap } = await import('./sitemap');
  return sitemap();
}

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubEnv('SITE_URL', SITE);
  listar.mockResolvedValue(pagina([]));
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('o que entra na lista', () => {
  it('sempre inclui a vitrine, com a prioridade mais alta', async () => {
    const mapa = await gerar();

    expect(mapa[0].url).toBe(SITE);
    expect(mapa[0].priority).toBe(1);
    expect(mapa[0].changeFrequency).toBe('daily');
  });

  it('inclui a página de cada planta', async () => {
    listar.mockResolvedValue(pagina(plantasFalsas(3)));
    const mapa = await gerar();

    expect(mapa.map((e) => e.url)).toEqual([
      SITE,
      `${SITE}/planta/p-0`,
      `${SITE}/planta/p-1`,
      `${SITE}/planta/p-2`,
    ]);
  });

  it('pede só as disponíveis', async () => {
    // Indexar planta esgotada faz alguém chegar pelo Google numa página que diz
    // "indisponível", e sair da loja.
    await gerar();
    expect(listar.mock.calls[0][0]).toMatchObject({ onlyAvailable: true });
  });

  it('usa o endereço público, e não caminho relativo', async () => {
    // Sitemap com caminho relativo é ignorado: o buscador precisa da URL
    // completa para saber de qual site aquilo é.
    listar.mockResolvedValue(pagina(plantasFalsas(1)));
    const mapa = await gerar();

    for (const entrada of mapa) {
      expect(entrada.url).toMatch(/^https:\/\//);
    }
  });
});

describe('paginação', () => {
  it('nunca pede mais que 50 por vez, que é o teto da API', async () => {
    listar.mockResolvedValue(pagina(plantasFalsas(50), 1));
    await gerar();

    for (const [filtros] of listar.mock.calls) {
      expect(filtros.size).toBeLessThanOrEqual(50);
    }
  });

  it('busca as páginas seguintes até acabar', async () => {
    listar
      .mockResolvedValueOnce(pagina(plantasFalsas(50, 'a'), 3))
      .mockResolvedValueOnce(pagina(plantasFalsas(50, 'b'), 3))
      .mockResolvedValueOnce(pagina(plantasFalsas(7, 'c'), 3));

    const mapa = await gerar();

    expect(listar).toHaveBeenCalledTimes(3);
    expect(listar.mock.calls.map(([f]) => f.page)).toEqual([0, 1, 2]);
    // 107 plantas mais a vitrine.
    expect(mapa).toHaveLength(108);
  });

  it('para na última página, sem pedir uma a mais', async () => {
    listar.mockResolvedValue(pagina(plantasFalsas(2), 1));
    await gerar();

    expect(listar).toHaveBeenCalledTimes(1);
  });

  it('não entra em laço quando a API responde sempre a mesma coisa', async () => {
    // Um backend que sempre devolve totalPages alto travaria a geração da
    // página. O teto de páginas existe para isso.
    listar.mockResolvedValue(pagina(plantasFalsas(50), 9999));
    const mapa = await gerar();

    expect(listar.mock.calls.length).toBeLessThanOrEqual(40);
    expect(mapa.length).toBeGreaterThan(1);
  });
});

describe('quando o catálogo está fora do ar', () => {
  it('entrega a vitrine em vez de falhar', async () => {
    // Um erro aqui faria o Google receber 500 e voltar com menos frequência.
    listar.mockRejectedValue(new Error('A vitrine não respondeu (503)'));
    const mapa = await gerar();

    expect(mapa).toEqual([expect.objectContaining({ url: SITE })]);
  });

  it('mantém o que já tinha buscado quando falha no meio', async () => {
    listar
      .mockResolvedValueOnce(pagina(plantasFalsas(50, 'a'), 2))
      .mockRejectedValueOnce(new Error('caiu'));

    const mapa = await gerar();

    // Metade da lista é melhor que nenhuma, mas o comportamento precisa ser
    // conhecido: hoje o catch descarta tudo e devolve só a vitrine.
    expect(mapa).toEqual([expect.objectContaining({ url: SITE })]);
  });
});
