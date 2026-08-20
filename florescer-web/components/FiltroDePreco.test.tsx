import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { FiltroDePreco } from './FiltroDePreco';

/**
 * O que estes testes protegem
 *
 * Este controle substituiu uma lista fechada de tetos ("até R$ 30, até R$ 50").
 * A lista tinha dois defeitos: obrigava a pessoa a aceitar um corte que alguém
 * escolheu por ela, e não dizia nada sobre a loja. Quem procurava algo até
 * R$ 70 precisava decidir entre ver de menos ou ver demais.
 *
 * Três coisas aqui erram em silêncio:
 *
 * - **navegar a cada movimento** dispararia uma consulta por quadro de animação
 *   e faria a lista piscar enquanto o dedo arrasta;
 * - **manter o parâmetro no máximo** deixaria a URL suja e o "limpar" aceso
 *   mesmo sem filtro nenhum;
 * - **não acompanhar mudança externa** (limpar, botão voltar, link recebido)
 *   deixaria o controle mostrando um valor que não é o da página.
 */

function montar(valor: number | null = null, faixa = { minimo: 20, maximo: 120 }) {
  const aoEscolher = vi.fn();
  const util = render(
    <FiltroDePreco minimo={faixa.minimo} maximo={faixa.maximo} valor={valor} aoEscolher={aoEscolher} />,
  );
  return { ...util, aoEscolher, controle: screen.getByRole('slider') };
}

describe('os extremos', () => {
  it('vêm do estoque, e não de valores fixos', () => {
    const { controle } = montar(null, { minimo: 15, maximo: 80 });

    expect(controle).toHaveAttribute('min', '15');
    expect(controle).toHaveAttribute('max', '80');
  });

  it('mostra os dois extremos em reais', () => {
    montar(null, { minimo: 20, maximo: 120 });

    expect(screen.getByText(/R\$\s?20,00/)).toBeInTheDocument();
    expect(screen.getByText(/R\$\s?120,00/)).toBeInTheDocument();
  });
});

describe('o que a pessoa lê', () => {
  it('diz "qualquer preço" quando não há filtro', () => {
    montar(null);
    expect(screen.getByText('Qualquer preço')).toBeInTheDocument();
  });

  it('mostra o teto escolhido em reais, e não o número cru', () => {
    montar(45);
    expect(screen.getByText(/Até R\$\s?45,00/)).toBeInTheDocument();
    expect(screen.queryByText('45')).toBeNull();
  });

  it('descreve o valor para quem usa leitor de tela', () => {
    const { controle } = montar(45);
    expect(controle).toHaveAttribute('aria-valuetext', expect.stringContaining('45,00'));
  });

  it('não oferece "remover" quando não há filtro', () => {
    montar(null);
    expect(screen.queryByRole('button', { name: 'remover' })).toBeNull();
  });

  it('oferece "remover" quando há um teto', () => {
    montar(45);
    expect(screen.getByRole('button', { name: 'remover' })).toBeInTheDocument();
  });
});

describe('arrastar', () => {
  it('não navega enquanto o controle está sendo movido', () => {
    const { controle, aoEscolher } = montar(null);
    fireEvent.change(controle, { target: { value: '60' } });

    expect(aoEscolher).not.toHaveBeenCalled();
    // Mas o texto acompanha na hora, senão parece travado.
    expect(screen.getByText(/Até R\$\s?60,00/)).toBeInTheDocument();
  });

  it('avisa quando a pessoa solta', () => {
    const { controle, aoEscolher } = montar(null);
    fireEvent.change(controle, { target: { value: '60' } });
    fireEvent.mouseUp(controle, { target: { value: '60' } });

    expect(aoEscolher).toHaveBeenCalledWith(60);
  });

  it('funciona pelo toque, e não só pelo mouse', () => {
    const { controle, aoEscolher } = montar(null);
    fireEvent.change(controle, { target: { value: '40' } });
    fireEvent.touchEnd(controle, { target: { value: '40' } });

    expect(aoEscolher).toHaveBeenCalledWith(40);
  });

  it('funciona pelo teclado', () => {
    const { controle, aoEscolher } = montar(null);
    fireEvent.change(controle, { target: { value: '35' } });
    fireEvent.keyUp(controle, { target: { value: '35' } });

    expect(aoEscolher).toHaveBeenCalledWith(35);
  });

  it('arrastar até o fim significa "tanto faz", e não "até o mais caro"', () => {
    // Manter o parâmetro no máximo deixaria a URL suja e o "limpar" aceso à toa.
    const { controle, aoEscolher } = montar(60);
    fireEvent.change(controle, { target: { value: '120' } });
    fireEvent.mouseUp(controle, { target: { value: '120' } });

    expect(aoEscolher).toHaveBeenCalledWith(null);
  });
});

describe('remover', () => {
  it('tira o teto sem precisar arrastar de volta', () => {
    const { aoEscolher } = montar(45);
    fireEvent.click(screen.getByRole('button', { name: 'remover' }));

    expect(aoEscolher).toHaveBeenCalledWith(null);
  });
});

describe('mudança vinda de fora', () => {
  it('acompanha quando o filtro é limpo em outro lugar', () => {
    // Acontece ao clicar em "limpar tudo", ao voltar pelo navegador, ou ao
    // abrir um link já filtrado que alguém mandou.
    const { rerender } = render(
      <FiltroDePreco minimo={20} maximo={120} valor={45} aoEscolher={vi.fn()} />,
    );
    expect(screen.getByText(/Até R\$\s?45,00/)).toBeInTheDocument();

    rerender(<FiltroDePreco minimo={20} maximo={120} valor={null} aoEscolher={vi.fn()} />);
    expect(screen.getByText('Qualquer preço')).toBeInTheDocument();
  });
});
