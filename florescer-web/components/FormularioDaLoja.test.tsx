import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { DadosDaLoja } from '@/lib/loja';
import { FormularioDaLoja } from './FormularioDaLoja';

/**
 * O que estes testes protegem
 *
 * Esta é a tela onde a vendedora troca o número que recebe os pedidos. Errar
 * aqui é o defeito mais caro do sistema inteiro: um número errado faz o botão
 * de comprar levar a uma conversa que não existe, e **nada na loja indica
 * isso**. Ela continua achando que está vendendo, e os pedidos somem.
 *
 * Por isso os casos cobrem, além do caminho feliz:
 *
 * - o aviso do backend aparecendo no campo certo, e não numa faixa genérica;
 * - a sessão vencida levando ao login em vez de perder o que ela digitou;
 * - o botão travando durante o envio, para dois cliques não virarem dois
 *   salvamentos;
 * - a confirmação visível, porque salvar sem retorno faz qualquer pessoa
 *   clicar de novo.
 */

const empurrar = vi.fn();
const atualizar = vi.fn();
const salvar = vi.fn();
const token = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: empurrar, refresh: atualizar }),
}));

vi.mock('@/lib/loja', async (original) => ({
  ...(await original<typeof import('@/lib/loja')>()),
  salvarDadosDaLoja: (...args: unknown[]) => salvar(...args),
}));

vi.mock('@/lib/sessao', () => ({
  lerToken: () => token(),
}));

const VAZIA: DadosDaLoja = {
  whatsappNumber: null,
  deliveryCity: null,
  instagramHandle: null,
  openingHours: null,
};

const PREENCHIDA: DadosDaLoja = {
  whatsappNumber: '5511987654321',
  deliveryCity: 'São Paulo, SP',
  instagramHandle: 'florescer.plantas',
  openingHours: 'Segunda a sábado, das 8h às 18h',
};

const ok = { ok: true, status: 200 } as Response;

const campo = (id: string) => document.getElementById(id) as HTMLInputElement;

function preencher(id: string, valor: string) {
  fireEvent.change(campo(id), { target: { value: valor } });
}

const salvarClicando = () => fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));

/** O corpo que foi mandado à API na última chamada. */
const enviado = () => salvar.mock.calls[0][0] as DadosDaLoja;

beforeEach(() => {
  vi.clearAllMocks();
  token.mockReturnValue('token.de.admin');
  salvar.mockResolvedValue(ok);
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('o que a tela mostra', () => {
  it('vem preenchida com o que já está salvo', () => {
    render(<FormularioDaLoja inicial={PREENCHIDA} />);

    expect(campo('whatsappNumber')).toHaveValue('5511987654321');
    expect(campo('deliveryCity')).toHaveValue('São Paulo, SP');
    expect(campo('instagramHandle')).toHaveValue('florescer.plantas');
    expect(campo('openingHours')).toHaveValue('Segunda a sábado, das 8h às 18h');
  });

  it('abre vazia quando nada foi configurado ainda', () => {
    render(<FormularioDaLoja inicial={VAZIA} />);
    expect(campo('whatsappNumber')).toHaveValue('');
  });

  it('explica o que acontece se o número ficar em branco', () => {
    // Sem esse aviso, ela apaga o número sem saber que o botão de comprar some
    // da loja inteira.
    render(<FormularioDaLoja inicial={VAZIA} />);
    expect(screen.getByText(/o botão some da loja/i)).toBeInTheDocument();
  });

  it('dá um exemplo de número, com país e DDD', () => {
    // "Digite o número" sem exemplo faz metade das pessoas esquecerem o 55.
    render(<FormularioDaLoja inicial={VAZIA} />);
    expect(screen.getByText(/\+55 \(11\) 98765-4321/)).toBeInTheDocument();
  });
});

describe('salvar', () => {
  it('manda os quatro campos', async () => {
    render(<FormularioDaLoja inicial={VAZIA} />);
    preencher('whatsappNumber', '5511987654321');
    preencher('deliveryCity', 'São Paulo, SP');
    preencher('instagramHandle', '@florescer.plantas');
    preencher('openingHours', 'Seg a sáb, 8h às 18h');
    salvarClicando();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(enviado()).toEqual({
      whatsappNumber: '5511987654321',
      deliveryCity: 'São Paulo, SP',
      instagramHandle: '@florescer.plantas',
      openingHours: 'Seg a sáb, 8h às 18h',
    });
  });

  it('manda o número como ela digitou, com pontuação e tudo', async () => {
    // Limpar aqui esconderia da API o que a pessoa realmente escreveu, e a
    // normalização precisa valer também para quem chama a API direto.
    render(<FormularioDaLoja inicial={VAZIA} />);
    preencher('whatsappNumber', '+55 (11) 98765-4321');
    salvarClicando();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(enviado().whatsappNumber).toBe('+55 (11) 98765-4321');
  });

  it('leva o token, senão a API recusa', async () => {
    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(salvar.mock.calls[0][1]).toBe('token.de.admin');
  });

  it('confirma na tela que salvou', async () => {
    // Salvar sem retorno visível faz qualquer pessoa clicar de novo, achando
    // que não funcionou.
    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent(/salvo/i));
  });

  it('manda a vitrine se atualizar, senão o rodapé segue com o valor antigo', async () => {
    // A vitrine lê estes dados no servidor: sem o refresh, ela salva e continua
    // vendo o número velho no site.
    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() => expect(atualizar).toHaveBeenCalled());
  });
});

describe('quando algo dá errado', () => {
  it('mostra o aviso no campo que a API apontou', async () => {
    salvar.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({
        error: 'Erro de validação',
        details: { whatsappNumber: 'Digite o número com o código do país e o DDD.' },
      }),
    } as Response);

    render(<FormularioDaLoja inicial={VAZIA} />);
    preencher('whatsappNumber', '123');
    salvarClicando();

    await waitFor(() =>
      expect(screen.getByText('Digite o número com o código do país e o DDD.')).toBeInTheDocument(),
    );
    // E fica ao lado do campo, e não numa faixa solta no topo.
    expect(campo('whatsappNumber').closest('div')).toHaveTextContent('Digite o número');
  });

  it('leva ao login quando a sessão venceu', async () => {
    salvar.mockResolvedValue({ ok: false, status: 401 } as Response);

    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/login'));
  });

  it('leva ao login quando nem havia sessão', async () => {
    token.mockReturnValue(null);

    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/login'));
    expect(salvar).not.toHaveBeenCalled();
  });

  it('avisa quando a rede falha, e deixa tentar de novo', async () => {
    salvar.mockRejectedValue(new TypeError('Failed to fetch'));

    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/não foi possível salvar/i),
    );
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeEnabled();
  });
});

describe('envio duplo', () => {
  it('trava o botão enquanto salva', async () => {
    let liberar: (r: Response) => void = () => {};
    salvar.mockReturnValue(
      new Promise<Response>((resolve) => {
        liberar = resolve;
      }),
    );

    render(<FormularioDaLoja inicial={VAZIA} />);
    salvarClicando();

    const botao = await screen.findByRole('button', { name: 'Salvando...' });
    expect(botao).toBeDisabled();

    fireEvent.click(botao);
    expect(salvar).toHaveBeenCalledTimes(1);

    liberar(ok);
    await waitFor(() => expect(screen.getByRole('status')).toBeInTheDocument());
  });
});
