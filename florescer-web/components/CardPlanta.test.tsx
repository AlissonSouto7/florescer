import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { PLANTA } from '@/test/fixtures';
import { imagemFalsa, linkFalso } from '@/test/next-mocks';
import { CardPlanta } from './CardPlanta';

/**
 * O que estes testes protegem
 *
 * O cartão é o que decide o clique. Dois riscos concretos:
 *
 * - a foto: se o caminho parar de ser convertido, a imagem some sem erro na
 *   tela, e uma vitrine sem foto não vende;
 * - o selo de esgotada: sem ele, a pessoa abre o detalhe, encontra "planta
 *   indisponível" e desiste da loja em vez da planta.
 *
 * Os campos nulos existem de verdade no banco: plantas cadastradas antes de
 * altura, luz e "segura para pets" passarem a ser exigidas. O cartão precisa
 * omitir o que não sabe, e não escrever "null" na tela.
 */

vi.mock('next/image', () => imagemFalsa());

vi.mock('next/link', () => linkFalso());

/**
 * O Intl separa o R$ do valor com espaço NÃO separável (U+00A0), e não com
 * espaço comum. Procurar pelo espaço comum não encontra o texto, mesmo com ele
 * visível na tela.
 */
const PRECO = /^R\$\s49,90$/;

/** A loja tem número configurado na maioria dos casos. */
const NUMERO = '5511987654321';

describe('o que o cartão mostra', () => {
  it('leva para o detalhe da planta', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    // O nome é o link para o detalhe; o cartão tem outro link, o do WhatsApp.
    expect(screen.getByRole('link', { name: 'Espada de São Jorge' }))
      .toHaveAttribute('href', '/planta/planta-1');
  });

  it('mostra nome e preço em real', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(screen.getByText('Espada de São Jorge')).toBeInTheDocument();
    expect(screen.getByText(PRECO)).toBeInTheDocument();
  });

  it('usa o caminho do nosso domínio, não o host que a API sugeriu', () => {
    // A API devolve product-service:8081, um endereço que só existe dentro da
    // rede do Docker. Servido assim, o navegador não alcança a foto.
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', '/uploads/a.png');
  });

  it('descreve a foto pelo nome da planta, para quem usa leitor de tela', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(screen.getByAltText('Espada de São Jorge')).toBeInTheDocument();
  });

  it('mostra tamanho e luz, que é o que a pessoa quer saber antes de abrir', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(screen.getByText('40 cm')).toBeInTheDocument();
    expect(screen.getByText('Meia sombra')).toBeInTheDocument();
  });
});

describe('atalho do WhatsApp', () => {
  const atalho = () => screen.queryByRole('link', { name: /Comprar .* pelo WhatsApp/ });

  it('oferece a conversa direto do cartão', () => {
    // Quem já reconhece a planta pela foto não precisa abrir outra página só
    // para começar a falar com a vendedora.
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);

    expect(atalho()).toHaveAttribute('href', expect.stringContaining(`wa.me/${NUMERO}`));
  });

  it('a mensagem já leva o nome da planta', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);

    const texto = new URL(atalho()!.getAttribute('href')!).searchParams.get('text');
    expect(texto).toContain('Espada de São Jorge');
  });

  it('some quando a loja ainda não tem número', () => {
    // Um link para wa.me sem número abre uma página de erro do WhatsApp.
    render(<CardPlanta planta={PLANTA} numero={null} />);
    expect(atalho()).toBeNull();
  });

  it('some em planta esgotada', () => {
    // O pedido chegaria para uma venda que ela não pode atender.
    render(<CardPlanta planta={{ ...PLANTA, quantityStock: 0 }} numero={NUMERO} />);
    expect(atalho()).toBeNull();
  });

  it('diz de qual planta se trata, para quem usa leitor de tela', () => {
    // Numa lista de doze plantas, doze links iguais dizendo "WhatsApp" não
    // ajudam ninguém a saber qual está sendo aberto.
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(atalho()).toHaveAccessibleName('Comprar Espada de São Jorge pelo WhatsApp');
  });
});

describe('segurança para animais', () => {
  it('mostra o selo quando a planta é segura', () => {
    render(<CardPlanta planta={{ ...PLANTA, petSafe: true }}  numero={NUMERO} />);
    expect(screen.getByText('Segura para pets')).toBeInTheDocument();
  });

  it('não escreve "tóxica" no cartão', () => {
    // Letra pequena que assusta sem explicar. O aviso completo fica no detalhe.
    render(<CardPlanta planta={{ ...PLANTA, petSafe: false }}  numero={NUMERO} />);
    expect(screen.queryByText(/tóxica/i)).toBeNull();
    expect(screen.queryByText('Segura para pets')).toBeNull();
  });
});

describe('planta esgotada', () => {
  it('avisa quando o estoque zerou', () => {
    render(<CardPlanta planta={{ ...PLANTA, quantityStock: 0 }}  numero={NUMERO} />);
    expect(screen.getByText('Esgotada')).toBeInTheDocument();
  });

  it('avisa quando a vendedora tirou de venda', () => {
    render(<CardPlanta planta={{ ...PLANTA, availability: false }}  numero={NUMERO} />);
    expect(screen.getByText('Esgotada')).toBeInTheDocument();
  });

  it('não avisa nada quando há estoque', () => {
    render(<CardPlanta planta={PLANTA} numero={NUMERO} />);
    expect(screen.queryByText('Esgotada')).toBeNull();
  });
});

describe('planta cadastrada antes dos campos novos', () => {
  it('omite o que não sabe, em vez de escrever "null" na tela', () => {
    render(<CardPlanta planta={{ ...PLANTA, heightCm: null, light: null, petSafe: null }}  numero={NUMERO} />);

    expect(screen.queryByText(/null/i)).toBeNull();
    expect(screen.queryByText('Segura para pets')).toBeNull();
    // O essencial continua: nome, preço e o link para o detalhe.
    expect(screen.getByText('Espada de São Jorge')).toBeInTheDocument();
    expect(screen.getByText(PRECO)).toBeInTheDocument();
  });

  it('cai no placeholder quando não há foto', () => {
    render(<CardPlanta planta={{ ...PLANTA, imageUrl: '' }}  numero={NUMERO} />);
    expect(screen.getByRole('img')).toHaveAttribute('src', '/placeholder.svg');
  });
});
