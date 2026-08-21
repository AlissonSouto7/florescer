import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { MenuDaVendedora } from './MenuDaVendedora';

/**
 * O que estes testes protegem
 *
 * Este menu decide o que o cabeçalho mostra, e erra de dois jeitos opostos:
 *
 * - **mostrar para quem não é vendedora** enche a loja de um link que não leva a
 *   lugar nenhum útil, e faz a vitrine parecer sistema interno;
 * - **não mostrar para quem acabou de entrar** deixa a vendedora sem saída
 *   visível, achando que o login não funcionou.
 *
 * O segundo caso aconteceu de verdade e está coberto abaixo, no bloco sobre
 * navegação. Ele não aparece em teste isolado do componente: só existe na
 * costura entre login, navegação e layout, e foi encontrado percorrendo a loja
 * no navegador.
 *
 * Nada disto é segurança. Quem protege o painel é o backend, que recusa
 * requisição sem token de ADMIN. Esconder o link é sobre não poluir a
 * experiência de quem veio comprar uma planta.
 */

const empurrar = vi.fn();
const token = vi.fn();
const admin = vi.fn();
const venceu = vi.fn();
const esquecer = vi.fn();

let caminho = '/';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: empurrar, refresh: vi.fn() }),
  usePathname: () => caminho,
}));

vi.mock('@/lib/sessao', () => ({
  lerToken: () => token(),
  ehAdmin: (t: string) => admin(t),
  expirado: (t: string) => venceu(t),
  esquecerToken: () => esquecer(),
}));

/** Estado de quem entrou como vendedora. */
function comSessaoValida() {
  token.mockReturnValue('token.de.admin');
  admin.mockReturnValue(true);
  venceu.mockReturnValue(false);
}

const menuVisivel = () => screen.queryByRole('link', { name: 'Minhas plantas' }) !== null;

beforeEach(() => {
  vi.clearAllMocks();
  caminho = '/';
  token.mockReturnValue(null);
  admin.mockReturnValue(false);
  venceu.mockReturnValue(true);
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('quem vê o menu', () => {
  it('mostra para quem entrou como vendedora', async () => {
    comSessaoValida();
    render(<MenuDaVendedora />);

    await waitFor(() => expect(menuVisivel()).toBe(true));
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument();
  });

  it('não mostra nada para quem só veio comprar', () => {
    const { container } = render(<MenuDaVendedora />);
    expect(menuVisivel()).toBe(false);
    expect(container.querySelector('button')).toBeNull();
  });

  it('não mostra para conta autenticada que não é ADMIN', () => {
    // Autenticar não é o mesmo que administrar: uma conta comum entraria numa
    // tela onde tudo dá erro de permissão.
    token.mockReturnValue('token.de.usuario');
    admin.mockReturnValue(false);
    venceu.mockReturnValue(false);
    render(<MenuDaVendedora />);

    expect(menuVisivel()).toBe(false);
  });

  it('não mostra quando a sessão venceu', () => {
    token.mockReturnValue('token.velho');
    admin.mockReturnValue(true);
    venceu.mockReturnValue(true);
    render(<MenuDaVendedora />);

    expect(menuVisivel()).toBe(false);
  });
});

describe('ao mudar de página', () => {
  it('reavalia a sessão, e não fica preso na primeira verificação', async () => {
    /**
     * Este é o caso que motivou o teste.
     *
     * O layout não é remontado ao navegar: o Next troca só o conteúdo. Com a
     * lista de dependências vazia, a verificação rodava uma vez, na primeira
     * página aberta, e nunca mais. Quem entrava pelo login chegava ao painel com
     * o cabeçalho ainda dizendo que não havia sessão, e só um F5 corrigia.
     */
    caminho = '/login';
    const { rerender } = render(<MenuDaVendedora />);
    expect(menuVisivel()).toBe(false);

    // O login guardou o token e navegou para o painel, sem remontar o layout.
    comSessaoValida();
    caminho = '/admin';
    rerender(<MenuDaVendedora />);

    await waitFor(() => expect(menuVisivel()).toBe(true));
  });

  it('some ao navegar depois que a sessão acabou', async () => {
    caminho = '/admin';
    comSessaoValida();
    const { rerender } = render(<MenuDaVendedora />);
    await waitFor(() => expect(menuVisivel()).toBe(true));

    // A sessão venceu, e a pessoa navegou para outra página.
    token.mockReturnValue(null);
    admin.mockReturnValue(false);
    venceu.mockReturnValue(true);
    caminho = '/';
    rerender(<MenuDaVendedora />);

    await waitFor(() => expect(menuVisivel()).toBe(false));
  });

  it('continua visível se a sessão vence sem a pessoa sair da página', async () => {
    /**
     * Limitação conhecida, e aceita.
     *
     * A verificação acontece ao trocar de rota. Se o token vence com a pessoa
     * parada na mesma tela, o menu segue lá até a próxima navegação. Não é
     * problema de segurança: ao clicar em qualquer coisa, o backend recusa e ela
     * cai no login.
     *
     * Corrigir exigiria um temporizador vigiando a expiração, o que gasta para
     * resolver um incômodo de poucos segundos. Este caso existe para que a
     * escolha fique registrada, e para que mudá-la seja decisão, e não acidente.
     */
    caminho = '/admin';
    comSessaoValida();
    const { rerender } = render(<MenuDaVendedora />);
    await waitFor(() => expect(menuVisivel()).toBe(true));

    token.mockReturnValue(null);
    venceu.mockReturnValue(true);
    rerender(<MenuDaVendedora />);

    expect(menuVisivel()).toBe(true);
  });
});

describe('sair', () => {
  it('apaga a sessão, volta para a vitrine e esconde o menu', async () => {
    comSessaoValida();
    render(<MenuDaVendedora />);

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));

    expect(esquecer).toHaveBeenCalled();
    expect(empurrar).toHaveBeenCalledWith('/');
    // O menu precisa sumir na hora, sem depender de recarregar a página.
    await waitFor(() => expect(menuVisivel()).toBe(false));
  });
});
