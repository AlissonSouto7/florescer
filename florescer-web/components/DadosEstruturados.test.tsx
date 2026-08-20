import { render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PLANTA } from '@/test/fixtures';
import { DadosEstruturados } from './DadosEstruturados';

/**
 * O que estes testes protegem
 *
 * Este bloco é lido por máquina, e nada aqui aparece na tela. Um erro passa
 * despercebido para sempre: a página continua bonita, e o Google apenas deixa
 * de mostrar preço e foto no resultado, ou pior, mostra o preço errado.
 *
 * Dois casos merecem atenção especial:
 *
 * - **o preço** precisa ir como número com ponto (`49.90`), e não como o
 *   `R$ 49,90` que a pessoa lê. O formato brasileiro faz o buscador ler 49
 *   reais, ou descartar a oferta inteira;
 * - **a disponibilidade** precisa bater com a página. Anunciar como disponível
 *   uma planta esgotada leva alguém a clicar no resultado para encontrar
 *   "indisponível", e o Google penaliza dado que não corresponde ao conteúdo.
 */

const SITE = 'https://florescerplantas.com.br';

/** O JSON-LD que foi realmente escrito na página, já decodificado. */
function dadosDa(container: HTMLElement) {
  const bloco = container.querySelector('script[type="application/ld+json"]');
  expect(bloco, 'nenhum bloco de dados estruturados na página').not.toBeNull();
  // O `<` é escapado na saída; desfazer aqui para o parser aceitar.
  return JSON.parse(bloco!.innerHTML.replace(/\\u003c/g, '<'));
}

beforeEach(() => {
  vi.stubEnv('SITE_URL', SITE);
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('o que é declarado', () => {
  it('descreve a planta como um produto do schema.org', () => {
    const { container } = render(<DadosEstruturados planta={PLANTA} />);
    const dados = dadosDa(container);

    expect(dados['@context']).toBe('https://schema.org');
    expect(dados['@type']).toBe('Product');
    expect(dados.name).toBe('Espada de São Jorge');
    expect(dados.description).toBe('Resistente');
    expect(dados.category).toBe('Folhagem');
  });

  it('aponta a foto e a página pelo endereço público', () => {
    const { container } = render(<DadosEstruturados planta={PLANTA} />);
    const dados = dadosDa(container);

    // A URL que a API devolve traz o host interno do Docker, que o buscador não
    // alcança. Precisa ser o domínio do site, com o caminho.
    expect(dados.image).toBe(`${SITE}/uploads/a.png`);
    expect(dados.offers.url).toBe(`${SITE}/planta/planta-1`);
    expect(dados.image).not.toContain('product-service');
  });
});

describe('preço', () => {
  it('vai como número com ponto, e não no formato que a pessoa lê', () => {
    const { container } = render(<DadosEstruturados planta={PLANTA} />);
    const dados = dadosDa(container);

    expect(dados.offers.price).toBe('49.90');
    expect(dados.offers.priceCurrency).toBe('BRL');
  });

  it('mantém as duas casas mesmo em valor redondo', () => {
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, price: 30 }} />);
    expect(dadosDa(container).offers.price).toBe('30.00');
  });

  it('não usa vírgula nem o símbolo da moeda', () => {
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, price: 1250.5 }} />);
    const preco = dadosDa(container).offers.price;

    expect(preco).toBe('1250.50');
    expect(preco).not.toContain(',');
    expect(preco).not.toContain('R$');
  });
});

describe('disponibilidade', () => {
  it('anuncia como disponível quando há estoque', () => {
    const { container } = render(<DadosEstruturados planta={PLANTA} />);
    expect(dadosDa(container).offers.availability).toBe('https://schema.org/InStock');
  });

  it('anuncia como esgotada quando o estoque zerou', () => {
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, quantityStock: 0 }} />);
    expect(dadosDa(container).offers.availability).toBe('https://schema.org/OutOfStock');
  });

  it('anuncia como esgotada quando a vendedora tirou de venda', () => {
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, availability: false }} />);
    expect(dadosDa(container).offers.availability).toBe('https://schema.org/OutOfStock');
  });
});

describe('conteúdo que poderia escapar da tag', () => {
  it('escapa "<" para um nome de planta não fechar o script', () => {
    // O dado vem do banco, e não de terceiros, mas o nome é digitado por uma
    // pessoa. Sem escapar, um nome com "</script>" fecharia a tag aqui e o
    // resto viraria HTML executável na página.
    const nomeHostil = 'Samambaia </script><img src=x onerror=alert(1)>';
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, name: nomeHostil }} />);

    const bruto = container.querySelector('script')!.innerHTML;
    expect(bruto).not.toContain('</script>');
    expect(container.querySelector('img')).toBeNull();
    // E o dado continua correto depois de decodificado.
    expect(dadosDa(container).name).toBe(nomeHostil);
  });

  it('mantém acento legível para o buscador', () => {
    const { container } = render(<DadosEstruturados planta={{ ...PLANTA, name: 'Peperômia viçosa' }} />);
    expect(dadosDa(container).name).toBe('Peperômia viçosa');
  });
});
