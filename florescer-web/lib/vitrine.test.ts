import { describe, expect, it } from 'vitest';

import type { Pagina, Planta } from './api';
import { PLANTA } from '@/test/fixtures';
import { TEXTO_DO_VAZIO, motivoDoVazio, paginaDaUrl, precoDaUrl } from './vitrine';

/**
 * O que estes testes protegem
 *
 * Uma tela vazia é o pior lugar para dar uma explicação errada, porque não sobra
 * nada na página para contradizê-la. Com `?page=999` a vitrine dizia "Nenhuma
 * planta com esses filtros. Tente afrouxar um deles" sem filtro nenhum
 * aplicado: pedia uma ação impossível e não oferecia caminho de volta, com doze
 * plantas no catálogo.
 */

function pagina(dados: Partial<Pagina<Planta>>): Pagina<Planta> {
  return { content: [], totalElements: 0, totalPages: 0, number: 0, ...dados };
}

describe('motivoDoVazio', () => {
  it('não é vazio quando há planta na página', () => {
    expect(motivoDoVazio(pagina({ content: [PLANTA], totalElements: 1 }), false)).toBeNull();
  });

  it('página além do fim, mesmo sem filtro nenhum', () => {
    // O caso relatado: `?page=999` com catálogo cheio.
    expect(motivoDoVazio(pagina({ totalElements: 12, number: 999 }), false)).toBe(
      'pagina-inexistente',
    );
  });

  it('página além do fim continua sendo página além do fim, com filtro', () => {
    // Existe resultado para o filtro, só não nesta página. Culpar o filtro
    // mandaria a pessoa afrouxar algo que está funcionando.
    expect(motivoDoVazio(pagina({ totalElements: 3, number: 7 }), true)).toBe(
      'pagina-inexistente',
    );
  });

  it('filtro que não casou com nada', () => {
    expect(motivoDoVazio(pagina({ totalElements: 0 }), true)).toBe('filtros');
  });

  it('loja sem plantas, quando não há filtro para culpar', () => {
    expect(motivoDoVazio(pagina({ totalElements: 0 }), false)).toBe('loja-sem-plantas');
  });
});

describe('o texto de cada caso', () => {
  it('cada motivo tem título e detalhe', () => {
    for (const [motivo, texto] of Object.entries(TEXTO_DO_VAZIO)) {
      expect(texto.titulo, motivo).toBeTruthy();
      expect(texto.detalhe, motivo).toBeTruthy();
    }
  });

  it('só o caso de filtro fala em afrouxar filtro', () => {
    // Era exatamente esta frase aparecendo nos três casos.
    expect(TEXTO_DO_VAZIO.filtros.detalhe).toMatch(/afrouxar/i);
    expect(TEXTO_DO_VAZIO['pagina-inexistente'].detalhe).not.toMatch(/afrouxar/i);
    expect(TEXTO_DO_VAZIO['loja-sem-plantas'].detalhe).not.toMatch(/afrouxar/i);
  });

  it('nenhum texto culpa o filtro quando não há filtro', () => {
    expect(TEXTO_DO_VAZIO['loja-sem-plantas'].titulo).not.toMatch(/filtro/i);
    expect(TEXTO_DO_VAZIO['pagina-inexistente'].titulo).not.toMatch(/filtro/i);
  });
});

describe('precoDaUrl', () => {
  it('aceita preço de verdade', () => {
    expect(precoDaUrl('60')).toBe(60);
    expect(precoDaUrl('49.9')).toBe(49.9);
  });

  it('recusa o que não é número', () => {
    // Virava NaN, e o NaN aparecia na barra, no leitor de tela e na chamada.
    expect(precoDaUrl('abc')).toBeNull();
    expect(precoDaUrl('60; DROP')).toBeNull();
    expect(precoDaUrl('Infinity')).toBeNull();
  });

  it('recusa zero e negativo', () => {
    // `?maxPrice=-5` chegava à API como filtro válido e esvaziava a vitrine.
    expect(precoDaUrl('-5')).toBeNull();
    expect(precoDaUrl('0')).toBeNull();
  });

  it('ausente é ausente', () => {
    expect(precoDaUrl(null)).toBeNull();
    expect(precoDaUrl(undefined)).toBeNull();
    expect(precoDaUrl('')).toBeNull();
  });
});

describe('paginaDaUrl', () => {
  it('aceita página válida', () => {
    expect(paginaDaUrl('3')).toBe(3);
    expect(paginaDaUrl('0')).toBe(0);
  });

  it('volta para a primeira quando o valor não serve', () => {
    expect(paginaDaUrl('-1')).toBe(0);
    expect(paginaDaUrl('2.5')).toBe(0);
    expect(paginaDaUrl('abc')).toBe(0);
    expect(paginaDaUrl(undefined)).toBe(0);
  });
});
