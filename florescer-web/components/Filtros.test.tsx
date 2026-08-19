import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Filtros } from './Filtros';

/**
 * O que estes testes protegem
 *
 * O filtro que para de funcionar não quebra a tela: a vitrine responde 200 e
 * mostra plantas, só que as erradas. Quem filtrou por "segura para cães e gatos"
 * recebe planta tóxica e não tem como saber que o filtro foi ignorado.
 *
 * O estado morar na URL é o que faz o filtro sobreviver ao recarregar, permite
 * mandar o link já filtrado para alguém, e faz o botão voltar desfazer um filtro
 * em vez de sair da vitrine. Cada caso abaixo verifica um pedaço disso.
 */

const empurrar = vi.fn();
let params = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: empurrar }),
  useSearchParams: () => params,
}));

/** A URL para onde a interação levou, já desmontada em parâmetros. */
function destino(): URL {
  const caminho = empurrar.mock.calls.at(-1)?.[0] as string;
  return new URL(caminho, 'http://loja.local');
}

function comFiltros(query: string) {
  params = new URLSearchParams(query);
  return render(<Filtros />);
}

beforeEach(() => {
  vi.clearAllMocks();
  params = new URLSearchParams();
});

describe('escolher um filtro', () => {
  it('coloca a escolha na URL', () => {
    comFiltros('');
    fireEvent.click(screen.getByRole('button', { name: 'Meia sombra' }));

    expect(destino().searchParams.get('light')).toBe('MEIA_SOMBRA');
  });

  it('preserva os filtros que já estavam escolhidos', () => {
    comFiltros('light=SOMBRA');
    fireEvent.click(screen.getByRole('button', { name: 'Fácil de cuidar' }));

    const p = destino().searchParams;
    expect(p.get('light')).toBe('SOMBRA');
    expect(p.get('difficulty')).toBe('FACIL');
  });

  it('volta para a primeira página ao trocar de filtro', () => {
    // Continuar na página 3 de um resultado que agora tem uma página só
    // mostraria a vitrine vazia, e pareceria que não existe planta assim.
    comFiltros('page=3&light=SOMBRA');
    fireEvent.click(screen.getByRole('button', { name: 'Sol pleno' }));

    expect(destino().searchParams.has('page')).toBe(false);
  });

  it('marca visualmente a opção escolhida', () => {
    comFiltros('light=SOL_PLENO');
    expect(screen.getByRole('button', { name: 'Sol pleno' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Sombra' })).toHaveAttribute('aria-pressed', 'false');
  });
});

describe('desfazer', () => {
  it('clicar na opção já escolhida desmarca', () => {
    // É como a pessoa espera desfazer, sem procurar o "limpar".
    comFiltros('light=SOMBRA');
    fireEvent.click(screen.getByRole('button', { name: 'Sombra' }));

    expect(destino().searchParams.has('light')).toBe(false);
  });

  it('limpar volta a vitrine inteira', () => {
    comFiltros('light=SOMBRA&petSafe=true&maxPrice=50');
    fireEvent.click(screen.getByRole('button', { name: 'limpar' }));

    expect(empurrar).toHaveBeenCalledWith('/');
  });

  it('não oferece "limpar" quando não há filtro', () => {
    comFiltros('');
    expect(screen.queryByRole('button', { name: 'limpar' })).toBeNull();
  });

  it('não oferece "limpar" quando só há paginação', () => {
    // Estar na página 2 não é um filtro para limpar.
    comFiltros('page=2');
    expect(screen.queryByRole('button', { name: 'limpar' })).toBeNull();
  });

  it('oferece "limpar" assim que existe um filtro de verdade', () => {
    comFiltros('petSafe=true');
    expect(screen.getByRole('button', { name: 'limpar' })).toBeInTheDocument();
  });
});

describe('marcadores', () => {
  it('liga o filtro de segurança para animais', () => {
    comFiltros('');
    fireEvent.click(screen.getByLabelText(/Segura para cães e gatos/));

    expect(destino().searchParams.get('petSafe')).toBe('true');
  });

  it('desligar remove o parâmetro em vez de mandar false', () => {
    // petSafe=false devolveria só as tóxicas, o oposto do que a pessoa quer.
    comFiltros('petSafe=true');
    fireEvent.click(screen.getByLabelText(/Segura para cães e gatos/));

    expect(destino().searchParams.has('petSafe')).toBe(false);
  });

  it('reflete na tela o filtro que veio da URL', () => {
    // Quem abre um link já filtrado precisa ver quais filtros estão valendo.
    comFiltros('petSafe=true&onlyAvailable=true');
    expect(screen.getByLabelText(/Segura para cães e gatos/)).toBeChecked();
    expect(screen.getByLabelText(/Só as disponíveis/)).toBeChecked();
  });
});

describe('faixa de preço', () => {
  it('aplica o teto escolhido', () => {
    comFiltros('');
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '50' } });

    expect(destino().searchParams.get('maxPrice')).toBe('50');
  });

  it('"qualquer preço" remove o teto', () => {
    comFiltros('maxPrice=50');
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '' } });

    expect(destino().searchParams.has('maxPrice')).toBe(false);
  });
});

describe('opções oferecidas', () => {
  it('não oferece "Dentro ou fora" como filtro de ambiente', () => {
    // Filtrar por AMBOS não faz sentido para quem compra: quem quer planta de
    // dentro escolhe "Dentro de casa", e o backend já inclui as que servem para
    // os dois. Oferecer a opção sugeriria um terceiro grupo que não existe.
    comFiltros('');
    expect(screen.queryByRole('button', { name: 'Dentro ou fora' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Dentro de casa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Área externa' })).toBeInTheDocument();
  });

  it('oferece as três faixas de luz', () => {
    comFiltros('');
    for (const rotulo of ['Sol pleno', 'Meia sombra', 'Sombra']) {
      expect(screen.getByRole('button', { name: rotulo })).toBeInTheDocument();
    }
  });
});
