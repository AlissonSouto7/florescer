import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PLANTA } from '@/test/fixtures';
import { linkDeCompra, numeroDaLoja, podeComprar } from './whatsapp';

/**
 * O que estes testes protegem
 *
 * Este link é o único caminho entre quem quer comprar e quem vende, e agora
 * aparece em dois lugares: o botão grande da página de detalhe e o atalho no
 * cartão da vitrine. Tudo o que ele faz de errado custa uma venda, sem nada
 * aparecer na tela:
 *
 * - mensagem sem codificar: acento vira lixo e o "&" de um nome corta o texto,
 *   então a vendedora recebe "Tenho interesse na Espada de S" e não sabe qual
 *   planta é;
 * - link em planta esgotada: chega pedido que ela não pode atender;
 * - número ausente: `wa.me/` sem número abre uma página de erro do WhatsApp.
 */

const NUMERO = '5573998149668';

beforeEach(() => {
  vi.stubEnv('WHATSAPP_NUMBER', NUMERO);
});

afterEach(() => {
  vi.unstubAllEnvs();
});

/** O texto que a vendedora vai receber, já decodificado. */
function mensagemDe(link: string): string {
  return new URL(link).searchParams.get('text') ?? '';
}

describe('numeroDaLoja', () => {
  it('devolve o número configurado', () => {
    expect(numeroDaLoja()).toBe(NUMERO);
  });

  it('devolve vazio quando não há número', () => {
    vi.stubEnv('WHATSAPP_NUMBER', '');
    expect(numeroDaLoja()).toBe('');
  });
});

describe('podeComprar', () => {
  it('sim quando está à venda e tem estoque', () => {
    expect(podeComprar(PLANTA)).toBe(true);
  });

  it('não quando o estoque zerou', () => {
    expect(podeComprar({ ...PLANTA, quantityStock: 0 })).toBe(false);
  });

  it('não quando a vendedora tirou de venda', () => {
    expect(podeComprar({ ...PLANTA, availability: false })).toBe(false);
  });

  it('exige as duas condições, e não uma delas', () => {
    // availability true com estoque 0 é o estado de quem vendeu a última e
    // ainda não desmarcou.
    expect(podeComprar({ ...PLANTA, availability: true, quantityStock: 0 })).toBe(false);
  });
});

describe('linkDeCompra', () => {
  it('aponta para o número da loja', () => {
    const link = linkDeCompra(PLANTA)!;
    expect(new URL(link).hostname).toBe('wa.me');
    expect(new URL(link).pathname).toBe(`/${NUMERO}`);
  });

  it('já diz qual planta e por quanto', () => {
    expect(mensagemDe(linkDeCompra(PLANTA)!)).toBe(
      'Olá! Tenho interesse na Espada de São Jorge (R$ 49,90) que vi no site.',
    );
  });

  it('não corrompe acento', () => {
    const link = linkDeCompra({ ...PLANTA, name: 'Peperômia viçosa' })!;
    expect(mensagemDe(link)).toContain('Peperômia viçosa');
    expect(link).toContain('vi%C3%A7osa');
  });

  it('não deixa "&" no nome cortar a mensagem ao meio', () => {
    // Sem encodeURIComponent, o "&" viraria separador de parâmetro e tudo
    // depois dele sumiria da mensagem.
    const link = linkDeCompra({ ...PLANTA, name: 'Costela & Jiboia' })!;
    expect(mensagemDe(link)).toContain('Costela & Jiboia');
    expect(mensagemDe(link)).toContain('que vi no site.');
  });

  it('não deixa "#" truncar a URL', () => {
    const link = linkDeCompra({ ...PLANTA, name: 'Cacto #7' })!;
    expect(mensagemDe(link)).toContain('Cacto #7');
  });

  it('devolve nulo para planta esgotada', () => {
    expect(linkDeCompra({ ...PLANTA, quantityStock: 0 })).toBeNull();
  });

  it('devolve nulo quando não há número configurado', () => {
    vi.stubEnv('WHATSAPP_NUMBER', '');
    expect(linkDeCompra(PLANTA)).toBeNull();
  });

  it('escreve o preço como o Brasil escreve', () => {
    const link = linkDeCompra({ ...PLANTA, price: 1234.5 })!;
    expect(mensagemDe(link)).toContain('R$ 1.234,50');
  });
});
