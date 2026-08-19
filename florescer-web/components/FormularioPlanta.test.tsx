import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PLANTA } from '@/test/fixtures';
import { imagemFalsa } from '@/test/next-mocks';
import { FormularioPlanta } from './FormularioPlanta';

/**
 * O que estes testes protegem
 *
 * Este formulário é a tela que a vendedora usa. Cada falha aqui aparece para ela
 * como "não consigo cadastrar", sem pista do motivo:
 *
 * - preço: ela digita 49,90 como fala. Se a conversão sumir, o backend recebe
 *   NaN e responde erro de validação num campo que, para ela, está preenchido
 *   certo;
 * - marcadores: checkbox desmarcado não vai no FormData. Se o código deixar de
 *   converter a ausência em `false`, o campo vira undefined e "não é segura para
 *   pets" some, que é justamente o dado que precisa aparecer;
 * - foto no cadastro: o backend exige. Barrar antes evita uma ida à API para
 *   voltar com erro;
 * - envio duplo: dois cliques cadastram a mesma planta duas vezes.
 */

const empurrar = vi.fn();
const salvar = vi.fn();
const alterar = vi.fn();
const token = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: empurrar, refresh: vi.fn() }),
}));

vi.mock('next/image', () => imagemFalsa());

vi.mock('@/lib/api', async (original) => ({
  ...(await original<typeof import('@/lib/api')>()),
  salvarPlanta: (...args: unknown[]) => salvar(...args),
  alterarPlanta: (...args: unknown[]) => alterar(...args),
}));

vi.mock('@/lib/sessao', () => ({
  lerToken: () => token(),
}));

const ok = { ok: true, status: 200 } as Response;

const ROTULOS: Record<string, string> = {
  name: 'Nome',
  type: 'Tipo',
  description: 'Descrição',
  price: 'Preço',
  quantityStock: 'Quantas você tem',
  heightCm: 'Altura em centímetros',
  careRequirements: 'Outros cuidados',
};

/** Preenche o mínimo que o formulário exige de um cadastro novo. */
function preencher(campos: Record<string, string> = {}) {
  const valores: Record<string, string> = {
    name: 'Samambaia',
    type: 'Pendente',
    description: 'Verde e farta',
    price: '49,90',
    quantityStock: '2',
    heightCm: '35',
    careRequirements: 'Regar sem encharcar',
    ...campos,
  };

  for (const [id, valor] of Object.entries(valores)) {
    const campo = screen.getByLabelText(new RegExp(ROTULOS[id]), { selector: `#${id}` });
    fireEvent.change(campo, { target: { value: valor } });
  }
}

/** Anexa uma foto, obrigatória no cadastro. */
function anexarFoto() {
  const arquivo = new File(['conteudo'], 'planta.png', { type: 'image/png' });
  fireEvent.change(screen.getByLabelText(/Foto da planta/), { target: { files: [arquivo] } });
  return arquivo;
}

function enviar() {
  fireEvent.click(screen.getByRole('button', { name: /Cadastrar planta|Salvar alterações/ }));
}

/** O corpo JSON que o formulário mandou para a API. */
function dadosEnviados(mock: typeof salvar): Record<string, unknown> {
  return mock.mock.calls[0][0] as Record<string, unknown>;
}

beforeEach(() => {
  vi.clearAllMocks();
  token.mockReturnValue('token.valido.aqui');
  salvar.mockResolvedValue(ok);
  alterar.mockResolvedValue(ok);
  // createObjectURL não existe no jsdom, e a prévia da foto depende dele.
  URL.createObjectURL = vi.fn(() => 'blob:previa');
  URL.revokeObjectURL = vi.fn();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('preço', () => {
  it('aceita vírgula, como a pessoa fala', async () => {
    render(<FormularioPlanta />);
    preencher({ price: '49,90' });
    anexarFoto();
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(dadosEnviados(salvar).price).toBe(49.9);
  });

  it('aceita ponto também, para quem digita do jeito do computador', async () => {
    render(<FormularioPlanta />);
    preencher({ price: '49.90' });
    anexarFoto();
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(dadosEnviados(salvar).price).toBe(49.9);
  });

  it('manda número, nunca texto: o backend recusa string em campo decimal', async () => {
    render(<FormularioPlanta />);
    preencher({ price: '1250,00' });
    anexarFoto();
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(typeof dadosEnviados(salvar).price).toBe('number');
    expect(dadosEnviados(salvar).price).toBe(1250);
  });

  it('mostra o preço de uma planta existente com vírgula, e não com ponto', () => {
    render(<FormularioPlanta planta={PLANTA} />);
    expect(screen.getByLabelText(/Preço/, { selector: '#price' })).toHaveValue('49,9');
  });
});

describe('marcadores', () => {
  it('manda false explícito quando desmarcado, e não deixa o campo sumir', async () => {
    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    // "À venda" e "Vai com o vaso" vêm marcados; petSafe vem desmarcado.
    fireEvent.click(screen.getByLabelText(/À venda/));
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    const dados = dadosEnviados(salvar);
    expect(dados.availability).toBe(false);
    expect(dados.petSafe).toBe(false);
    expect(dados.includesPot).toBe(true);
  });

  it('deixa "segura para pets" desmarcado por padrão', () => {
    // Na dúvida, o aviso tem que aparecer: quem tem animal precisa dele.
    render(<FormularioPlanta />);
    expect(screen.getByLabelText(/Segura para cães e gatos/)).not.toBeChecked();
  });
});

describe('foto', () => {
  it('barra o cadastro sem foto antes de chamar a API', async () => {
    render(<FormularioPlanta />);
    preencher();
    enviar();

    await waitFor(() => expect(screen.getByText('Escolha uma foto da planta.')).toBeInTheDocument());
    expect(salvar).not.toHaveBeenCalled();
  });

  it('permite editar sem trocar a foto', async () => {
    render(<FormularioPlanta planta={PLANTA} />);
    enviar();

    await waitFor(() => expect(alterar).toHaveBeenCalled());
    expect(alterar.mock.calls[0][2]).toBeNull(); // nenhuma imagem nova
  });

  it('manda o arquivo escolhido junto com os dados', async () => {
    render(<FormularioPlanta />);
    preencher();
    const arquivo = anexarFoto();
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(salvar.mock.calls[0][1]).toBe(arquivo);
  });
});

describe('sessão', () => {
  it('manda para o login quando não há token, sem tentar salvar', async () => {
    token.mockReturnValue(null);
    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/login'));
    expect(salvar).not.toHaveBeenCalled();
  });

  it('manda para o login quando a API responde 401', async () => {
    salvar.mockResolvedValue({ ok: false, status: 401 } as Response);
    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/login'));
  });

  it('passa o token para a API', async () => {
    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() => expect(salvar).toHaveBeenCalled());
    expect(salvar.mock.calls[0][2]).toBe('token.valido.aqui');
  });
});

describe('erros da API', () => {
  it('mostra a mensagem no campo que a API apontou', async () => {
    salvar.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ error: 'Erro', details: { price: 'deve ser maior que zero' } }),
    } as Response);

    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() => expect(screen.getByText('deve ser maior que zero')).toBeInTheDocument());
    expect(empurrar).not.toHaveBeenCalled();
  });

  it('avisa quando a rede falha, em vez de travar no "Salvando..."', async () => {
    salvar.mockRejectedValue(new TypeError('Failed to fetch'));

    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() =>
      expect(screen.getByText('Não foi possível salvar. Verifique sua conexão.')).toBeInTheDocument(),
    );
    // O botão volta a funcionar: ela precisa poder tentar de novo.
    expect(screen.getByRole('button', { name: /Cadastrar planta/ })).toBeEnabled();
  });
});

describe('envio duplo', () => {
  it('desabilita o botão enquanto salva, para não cadastrar a planta duas vezes', async () => {
    let liberar: (r: Response) => void = () => {};
    salvar.mockReturnValue(
      new Promise<Response>((resolve) => {
        liberar = resolve;
      }),
    );

    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    const botao = await screen.findByRole('button', { name: 'Salvando...' });
    expect(botao).toBeDisabled();

    // Um segundo clique enquanto a primeira requisição está no ar não pode virar
    // uma segunda planta.
    fireEvent.click(botao);
    expect(salvar).toHaveBeenCalledTimes(1);

    liberar(ok);
    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/admin'));
  });
});

describe('depois de salvar', () => {
  it('volta para o painel', async () => {
    render(<FormularioPlanta />);
    preencher();
    anexarFoto();
    enviar();

    await waitFor(() => expect(empurrar).toHaveBeenCalledWith('/admin'));
  });
});
