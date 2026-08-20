import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { Select, type Opcao } from './Select';

/**
 * O que estes testes protegem
 *
 * Este componente troca o `<select>` nativo por um botão e uma lista, para que a
 * lista aberta tenha a cara da loja em vez da do Windows. O preço dessa troca é
 * que **tudo o que o navegador dava de graça precisou ser reescrito**, e é
 * exatamente aí que implementações caseiras falham:
 *
 * - quem navega por teclado fica preso, porque as setas não andam ou o Escape
 *   não fecha;
 * - quem usa leitor de tela não sabe que aquilo é uma lista, nem qual item está
 *   escolhido, porque falta um `aria-` qualquer;
 * - a lista fica aberta para sempre, porque clicar fora não fecha;
 * - e, no formulário, o valor escolhido não chega à API, porque um botão não
 *   participa do `FormData` como um `<select>` participa.
 *
 * Nenhuma dessas falhas aparece em quem usa mouse e enxerga. Por isso cada uma
 * tem caso próprio aqui.
 */

const OPCOES: Opcao[] = [
  { valor: 'SOL_PLENO', rotulo: 'Sol pleno', detalhe: 'Aguenta sol direto boa parte do dia' },
  { valor: 'MEIA_SOMBRA', rotulo: 'Meia sombra', detalhe: 'Claridade sim, sol direto não' },
  { valor: 'SOMBRA', rotulo: 'Sombra', detalhe: 'Vive bem longe da janela' },
];

function montar(props: Partial<React.ComponentProps<typeof Select>> = {}) {
  const aoMudar = vi.fn();
  const util = render(
    <Select
      opcoes={OPCOES}
      valor="MEIA_SOMBRA"
      aoMudar={aoMudar}
      rotuloAcessivel="Quanta luz"
      {...props}
    />,
  );
  return { ...util, aoMudar, botao: screen.getByRole('combobox') };
}

const lista = () => screen.queryByRole('listbox');
const itens = () => screen.getAllByRole('option');

describe('o campo fechado', () => {
  it('mostra o rótulo da opção escolhida, e não o valor cru', () => {
    // "MEIA_SOMBRA" na tela seria o enum vazando para quem compra.
    const { botao } = montar();
    expect(botao).toHaveTextContent('Meia sombra');
    expect(botao).not.toHaveTextContent('MEIA_SOMBRA');
  });

  it('se anuncia como uma lista fechada', () => {
    const { botao } = montar();
    expect(botao).toHaveAttribute('aria-haspopup', 'listbox');
    expect(botao).toHaveAttribute('aria-expanded', 'false');
    expect(botao).toHaveAccessibleName('Quanta luz');
  });

  it('não deixa a lista no DOM antes de abrir', () => {
    montar();
    expect(lista()).toBeNull();
  });
});

describe('abrir e escolher com o mouse', () => {
  it('abre ao clicar e mostra todas as opções', () => {
    const { botao } = montar();
    fireEvent.click(botao);

    expect(botao).toHaveAttribute('aria-expanded', 'true');
    expect(itens()).toHaveLength(3);
  });

  it('mostra o detalhe de cada opção, que é o que ajuda a escolher', () => {
    const { botao } = montar();
    fireEvent.click(botao);

    expect(screen.getByText('Claridade sim, sol direto não')).toBeInTheDocument();
  });

  it('marca qual está escolhida, para o leitor de tela e para o olho', () => {
    const { botao } = montar();
    fireEvent.click(botao);

    const escolhida = itens().find((o) => o.getAttribute('aria-selected') === 'true');
    expect(escolhida).toHaveTextContent('Meia sombra');
  });

  it('avisa a escolha e fecha', () => {
    const { botao, aoMudar } = montar();
    fireEvent.click(botao);
    fireEvent.click(screen.getByText('Sombra'));

    expect(aoMudar).toHaveBeenCalledWith('SOMBRA');
    expect(lista()).toBeNull();
  });

  it('fecha ao clicar de novo no campo', () => {
    const { botao } = montar();
    fireEvent.click(botao);
    fireEvent.click(botao);

    expect(lista()).toBeNull();
  });
});

describe('teclado', () => {
  it('abre com Enter, espaço e as setas', () => {
    for (const tecla of ['Enter', ' ', 'ArrowDown', 'ArrowUp']) {
      const { botao, unmount } = montar();
      fireEvent.keyDown(botao, { key: tecla });
      expect(lista(), `${tecla} não abriu`).not.toBeNull();
      unmount();
    }
  });

  it('anda pelas opções com as setas', () => {
    const { botao } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });

    // Abre no item escolhido (índice 1) e desce para o último.
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-2'));

    fireEvent.keyDown(botao, { key: 'ArrowUp' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-1'));
  });

  it('não passa das pontas', () => {
    // Sem o limite, o foco sai da lista e o teclado deixa de escolher qualquer
    // coisa, sem nada indicar o que houve.
    const { botao } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });

    for (let i = 0; i < 10; i++) fireEvent.keyDown(botao, { key: 'ArrowDown' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-2'));

    for (let i = 0; i < 10; i++) fireEvent.keyDown(botao, { key: 'ArrowUp' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-0'));
  });

  it('Home e End vão direto às pontas', () => {
    const { botao } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });

    fireEvent.keyDown(botao, { key: 'End' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-2'));

    fireEvent.keyDown(botao, { key: 'Home' });
    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-0'));
  });

  it('Enter escolhe a opção em foco', () => {
    const { botao, aoMudar } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    fireEvent.keyDown(botao, { key: 'Enter' });

    expect(aoMudar).toHaveBeenCalledWith('SOMBRA');
    expect(lista()).toBeNull();
  });

  it('Escape fecha sem escolher e devolve o foco ao campo', () => {
    const { botao, aoMudar } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    fireEvent.keyDown(botao, { key: 'Escape' });

    expect(aoMudar).not.toHaveBeenCalled();
    expect(lista()).toBeNull();
    // Sem devolver o foco, o Tab seguinte recomeçaria do topo da página.
    expect(botao).toHaveFocus();
  });

  it('Tab fecha a lista em vez de deixá-la aberta atrás', () => {
    const { botao } = montar();
    fireEvent.keyDown(botao, { key: 'ArrowDown' });
    fireEvent.keyDown(botao, { key: 'Tab' });

    expect(lista()).toBeNull();
  });
});

describe('clicar fora', () => {
  it('fecha a lista', () => {
    const { botao } = montar();
    fireEvent.click(botao);
    expect(lista()).not.toBeNull();

    fireEvent.mouseDown(document.body);
    expect(lista()).toBeNull();
  });

  it('não fecha ao clicar dentro da própria lista', () => {
    const { botao } = montar();
    fireEvent.click(botao);
    fireEvent.mouseDown(screen.getByRole('listbox'));

    expect(lista()).not.toBeNull();
  });
});

describe('rolar a página', () => {
  it('fecha a lista, como acontece com a nativa', () => {
    // Sem isso, a lista fica flutuando no lugar antigo enquanto o conteúdo
    // desliza por baixo, e ela some do campo a que pertence.
    const { botao } = montar();
    fireEvent.click(botao);
    expect(lista()).not.toBeNull();

    fireEvent.scroll(window);
    expect(lista()).toBeNull();
  });
});

describe('mouse sobre as opções', () => {
  it('move o foco junto com o ponteiro', () => {
    // Assim o Enter escolhe o item sob o mouse, e não um item distante que o
    // teclado tinha deixado marcado.
    const { botao } = montar();
    fireEvent.click(botao);
    fireEvent.mouseEnter(screen.getByText('Sombra'));

    expect(botao).toHaveAttribute('aria-activedescendant', expect.stringContaining('-opcao-2'));
  });
});

describe('dentro de um formulário', () => {
  it('leva o valor no campo oculto, senão ele não chega à API', () => {
    // Um botão não participa do FormData como um <select> participa: sem este
    // campo, o dado escolhido simplesmente não sai da tela.
    const { container } = render(
      <Select
        opcoes={OPCOES}
        valor="SOMBRA"
        aoMudar={vi.fn()}
        rotuloAcessivel="Luz"
        name="light"
      />,
    );

    const oculto = container.querySelector('input[type="hidden"]') as HTMLInputElement;
    expect(oculto).not.toBeNull();
    expect(oculto.name).toBe('light');
    expect(oculto.value).toBe('SOMBRA');
  });

  it('é lido pelo FormData com o valor certo', () => {
    render(
      <form data-testid="formulario">
        <Select opcoes={OPCOES} valor="SOL_PLENO" aoMudar={vi.fn()} rotuloAcessivel="Luz" name="light" />
      </form>,
    );

    const dados = new FormData(screen.getByTestId('formulario') as HTMLFormElement);
    expect(dados.get('light')).toBe('SOL_PLENO');
  });

  it('não cria campo oculto quando não é campo de formulário', () => {
    // Nos filtros da vitrine o estado vive na URL, e um campo a mais seria lixo.
    const { container } = montar();
    expect(container.querySelector('input[type="hidden"]')).toBeNull();
  });
});
