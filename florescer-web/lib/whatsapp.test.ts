import { describe, expect, it } from 'vitest';

import { PLANTA } from '@/test/fixtures';
import { linkDeCompra, podeComprar, temNumero } from './whatsapp';

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

const NUMERO = '5511987654321';

/** O texto que a vendedora vai receber, já decodificado. */
function mensagemDe(link: string): string {
  return new URL(link).searchParams.get('text') ?? '';
}

describe('temNumero', () => {
  it('reconhece um número configurado', () => {
    expect(temNumero(NUMERO)).toBe(true);
  });

  it('não aceita vazio, espaço em branco, nulo nem ausente', () => {
    // Os quatro chegam da API ou do formulário, e todos significam a mesma
    // coisa: a loja ainda não tem número, e o botão precisa sumir.
    expect(temNumero('')).toBe(false);
    expect(temNumero('   ')).toBe(false);
    expect(temNumero(null)).toBe(false);
    expect(temNumero(undefined)).toBe(false);
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
    const link = linkDeCompra(PLANTA, NUMERO)!;
    expect(new URL(link).hostname).toBe('wa.me');
    expect(new URL(link).pathname).toBe(`/${NUMERO}`);
  });

  it('já diz qual planta e por quanto', () => {
    expect(mensagemDe(linkDeCompra(PLANTA, NUMERO)!)).toBe(
      'Olá! Tenho interesse na Espada de São Jorge (R$ 49,90) que vi no site.',
    );
  });

  it('não corrompe acento', () => {
    const link = linkDeCompra({ ...PLANTA, name: 'Peperômia viçosa' }, NUMERO)!;
    expect(mensagemDe(link)).toContain('Peperômia viçosa');
    expect(link).toContain('vi%C3%A7osa');
  });

  it('não deixa "&" no nome cortar a mensagem ao meio', () => {
    // Sem encodeURIComponent, o "&" viraria separador de parâmetro e tudo
    // depois dele sumiria da mensagem.
    const link = linkDeCompra({ ...PLANTA, name: 'Costela & Jiboia' }, NUMERO)!;
    expect(mensagemDe(link)).toContain('Costela & Jiboia');
    expect(mensagemDe(link)).toContain('que vi no site.');
  });

  it('não deixa "#" truncar a URL', () => {
    const link = linkDeCompra({ ...PLANTA, name: 'Cacto #7' }, NUMERO)!;
    expect(mensagemDe(link)).toContain('Cacto #7');
  });

  it('devolve nulo para planta esgotada', () => {
    expect(linkDeCompra({ ...PLANTA, quantityStock: 0 }, NUMERO)).toBeNull();
  });

  it('devolve nulo quando a loja não tem número', () => {
    expect(linkDeCompra(PLANTA, null)).toBeNull();
    expect(linkDeCompra(PLANTA, '')).toBeNull();
  });

  it('escreve o preço como o Brasil escreve', () => {
    const link = linkDeCompra({ ...PLANTA, price: 1234.5 }, NUMERO)!;
    expect(mensagemDe(link)).toContain('R$ 1.234,50');
  });
});
