import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { larguraDaTela } from '@/vitest.setup';
import { Filtros } from './Filtros';

/**
 * O que estes testes protegem
 *
 * O filtro que para de funcionar não quebra a tela: a vitrine responde 200 e
 * mostra plantas, só que as erradas. Quem filtrou por "segura para cães e
 * gatos" recebe planta tóxica e não tem como saber que o filtro foi ignorado.
 *
 * O estado morar na URL é o que faz o filtro sobreviver ao recarregar, permite
 * mandar o link já filtrado para alguém, e faz o botão voltar desfazer um
 * filtro em vez de sair da vitrine.
 *
 * O bloco sobre a barra existe por um motivo específico: a versão anterior era
 * uma coluna sempre aberta, e no celular ela empurrava a primeira planta para
 * fora da tela. Numa loja isso é o contrário do que se quer, e os casos abaixo
 * fixam o comportamento novo para que ele não regrida sem ninguém perceber.
 */

const empurrar = vi.fn();
let params = new URLSearchParams();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: empurrar }),
  useSearchParams: () => params,
}));

/** A faixa vem do estoque real; nos testes, um intervalo previsível. */
const FAIXA = { minimo: 20, maximo: 120 };

/** A URL para onde a interação levou, já desmontada em parâmetros. */
function destino(): URL {
  const caminho = empurrar.mock.calls.at(-1)?.[0] as string;
  return new URL(caminho, 'http://loja.local');
}

function comFiltros(query: string, quantidade = 12) {
  params = new URLSearchParams(query);
  return render(<Filtros faixaDePreco={FAIXA} quantidade={quantidade} />);
}

const botaoFiltrar = () => screen.getByRole('button', { name: /^filtrar/i });
const abrir = () => fireEvent.click(botaoFiltrar());

const CELULAR = 390;
const COMPUTADOR = 1440;

beforeEach(() => {
  vi.clearAllMocks();
  params = new URLSearchParams();
  document.body.style.overflow = '';
  larguraDaTela(COMPUTADOR);
});

describe('a barra, antes de abrir', () => {
  it('não aponta para um painel que ainda não existe', () => {
    // `aria-controls` é uma referência por id: com o painel fechado ela apontava
    // para nada, e o leitor de tela anuncia um alvo que não está lá.
    comFiltros('');

    const alvo = botaoFiltrar().getAttribute('aria-controls');
    expect(alvo === null || document.getElementById(alvo) !== null).toBe(true);
  });

  it('não mostra os grupos de filtro na frente das plantas', () => {
    // Este é o caso que motivou o redesenho: a coluna sempre aberta empurrava a
    // vitrine para baixo da dobra no celular.
    comFiltros('');

    expect(screen.queryByText('Luz que recebe')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Meia sombra' })).toBeNull();
  });

  it('mostra quantos filtros estão valendo', () => {
    comFiltros('light=SOMBRA&petSafe=true');
    expect(botaoFiltrar()).toHaveTextContent('2');
  });

  it('não mostra contador quando não há filtro', () => {
    comFiltros('');
    expect(botaoFiltrar()).not.toHaveTextContent(/\d/);
  });

  it('lista o que está filtrado, em português, e não o valor cru', () => {
    comFiltros('light=MEIA_SOMBRA&petSafe=true&maxPrice=50');

    expect(screen.getByRole('button', { name: /Remover filtro Meia sombra/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Remover filtro Segura para pets/ })).toBeInTheDocument();
    expect(screen.queryByText('MEIA_SOMBRA')).toBeNull();
  });

  it('mostra chip para cada tipo de filtro, e não só para alguns', () => {
    comFiltros('environment=INTERNO&difficulty=FACIL&onlyAvailable=true');

    expect(screen.getByRole('button', { name: /Remover filtro Dentro de casa/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Remover filtro Fácil de cuidar/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Remover filtro Só disponíveis/ })).toBeInTheDocument();
  });

  it('ignora valor inválido na URL em vez de mostrar lixo', () => {
    // Alguém pode editar a URL à mão, ou um link antigo pode trazer um valor
    // que não existe mais. O chip não pode aparecer vazio.
    comFiltros('light=INVENTADO&environment=NAO_EXISTE');
    expect(botaoFiltrar()).not.toHaveTextContent(/\d/);
  });

  it('remove um filtro sem precisar abrir o painel', () => {
    comFiltros('light=SOMBRA&petSafe=true');
    fireEvent.click(screen.getByRole('button', { name: /Remover filtro Sombra/ }));

    expect(destino().searchParams.has('light')).toBe(false);
    // E mantém o outro: remover um não pode limpar tudo.
    expect(destino().searchParams.get('petSafe')).toBe('true');
  });

  it('só oferece "limpar tudo" quando há mais de um filtro', () => {
    comFiltros('light=SOMBRA');
    expect(screen.queryByRole('button', { name: 'limpar tudo' })).toBeNull();

    comFiltros('light=SOMBRA&petSafe=true');
    expect(screen.getByRole('button', { name: 'limpar tudo' })).toBeInTheDocument();
  });

  it('a paginação não conta como filtro', () => {
    // Estar na página 2 não é algo que a pessoa queira "remover".
    comFiltros('page=2');
    expect(botaoFiltrar()).not.toHaveTextContent(/\d/);
  });
});

describe('abrir e fechar', () => {
  it('mostra os grupos ao abrir', () => {
    comFiltros('');
    abrir();

    expect(screen.getAllByText('Luz que recebe').length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: 'Meia sombra' }).length).toBeGreaterThan(0);
  });

  it('anuncia o estado para quem usa leitor de tela', () => {
    comFiltros('');
    expect(botaoFiltrar()).toHaveAttribute('aria-expanded', 'false');

    abrir();
    expect(botaoFiltrar()).toHaveAttribute('aria-expanded', 'true');
  });

  it('a gaveta do celular diz quantas plantas sobraram', () => {
    // Sem isso, a pessoa fecha, olha, e abre de novo para ajustar.
    comFiltros('', 7);
    abrir();

    expect(screen.getByRole('button', { name: 'Ver 7 plantas' })).toBeInTheDocument();
  });

  it('fala no singular quando sobra uma só', () => {
    comFiltros('', 1);
    abrir();

    expect(screen.getByRole('button', { name: 'Ver 1 planta' })).toBeInTheDocument();
  });

  it('trava a rolagem da página enquanto a gaveta está aberta, no celular', () => {
    // Sem travar, o dedo desliza a lista de plantas atrás em vez do conteúdo.
    larguraDaTela(CELULAR);
    comFiltros('');
    expect(document.body.style.overflow).toBe('');

    abrir();
    expect(document.body.style.overflow).toBe('hidden');
  });

  it('devolve a rolagem ao fechar', () => {
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();
    fireEvent.click(screen.getByRole('button', { name: 'Fechar filtros' }));

    expect(document.body.style.overflow).toBe('');
  });

  it('NÃO trava a rolagem no computador, onde gaveta nenhuma existe', () => {
    // O defeito relatado: no computador o painel é embutido e a página precisa
    // continuar rolando. Travando, a barra de rolagem some, e a página inteira
    // pula 7px para o lado porque o container recentraliza. Medido no
    // navegador em 1440px: barra de 15px com o painel fechado, 0 com ele
    // aberto; o título saiu de 89px para 96px da borda.
    larguraDaTela(COMPUTADOR);
    comFiltros('');
    abrir();

    expect(document.body.style.overflow).toBe('');
  });

  it('solta a rolagem se a janela crescer com o painel aberto', () => {
    // Abrir no celular e girar para paisagem, ou arrastar a janela: a trava
    // ficaria presa até fechar o painel.
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();
    expect(document.body.style.overflow).toBe('hidden');

    larguraDaTela(COMPUTADOR);
    expect(document.body.style.overflow).toBe('');
  });

  it('Escape fecha, como qualquer camada sobreposta', () => {
    comFiltros('');
    abrir();
    fireEvent.keyDown(document, { key: 'Escape' });

    expect(screen.queryByText('Luz que recebe')).toBeNull();
  });
});

describe('a gaveta do celular', () => {
  it('leva o foco para dentro ao abrir', () => {
    // A gaveta se declara `aria-modal`, o que faz o leitor de tela esconder o
    // resto da página. Com o foco parado no botão de fora, a pessoa fica com o
    // cursor num lugar que o leitor considera inexistente.
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();

    const gaveta = screen.getByRole('dialog');
    expect(gaveta.contains(document.activeElement)).toBe(true);
  });

  /** Tudo que recebe foco dentro da gaveta, na ordem em que o Tab visita. */
  function focaveisDaGaveta() {
    const gaveta = screen.getByRole('dialog');
    return [
      ...gaveta.querySelectorAll<HTMLElement>(
        'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])',
      ),
    ];
  }

  it('do último, o Tab volta para o primeiro em vez de sair da gaveta', () => {
    // Medido no navegador antes da correção: 31 elementos atrás da gaveta
    // continuavam alcançáveis pelo Tab, enquanto o leitor de tela dizia que não
    // existiam.
    //
    // A verificação exige o destino exato, e não "continua dentro". O jsdom não
    // move foco sozinho no Tab, então "continua dentro" passaria com o foco
    // parado, sem armadilha nenhuma implementada. Aconteceu na primeira versão
    // deste teste.
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();

    const focaveis = focaveisDaGaveta();
    const primeiro = focaveis[0];
    const ultimo = focaveis[focaveis.length - 1];

    ultimo.focus();
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Tab' });

    expect(document.activeElement).toBe(primeiro);
  });

  it('do primeiro, Shift+Tab volta para o último', () => {
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();

    const focaveis = focaveisDaGaveta();
    const primeiro = focaveis[0];
    const ultimo = focaveis[focaveis.length - 1];

    primeiro.focus();
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Tab', shiftKey: true });

    expect(document.activeElement).toBe(ultimo);
  });

  it('devolve o foco ao botão que abriu, ao fechar', () => {
    // Sem isso o foco volta para o começo da página, e quem navega por teclado
    // recomeça do cabeçalho toda vez que fecha um filtro.
    larguraDaTela(CELULAR);
    comFiltros('');
    abrir();
    fireEvent.click(screen.getByRole('button', { name: 'Fechar filtros' }));

    expect(document.activeElement).toBe(botaoFiltrar());
  });

  it('fecha ao tocar fora dela', () => {
    // Tocar no escuro atrás é como se fecha qualquer gaveta num app.
    const { container } = comFiltros('');
    abrir();

    const fundo = container.querySelector('[aria-hidden="true"].absolute.inset-0');
    fireEvent.click(fundo!);

    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('fecha pelo botão que mostra o resultado', () => {
    comFiltros('', 5);
    abrir();
    fireEvent.click(screen.getByRole('button', { name: 'Ver 5 plantas' }));

    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('se anuncia como camada sobreposta', () => {
    comFiltros('');
    abrir();

    const gaveta = screen.getByRole('dialog');
    expect(gaveta).toHaveAttribute('aria-modal', 'true');
    expect(gaveta).toHaveAccessibleName('Filtros');
  });
});

describe('escolher um filtro', () => {
  /** Clica na primeira opção com aquele texto, venha da gaveta ou do painel. */
  const escolher = (rotulo: string) =>
    fireEvent.click(screen.getAllByRole('button', { name: rotulo })[0]);

  it('coloca a escolha na URL', () => {
    comFiltros('');
    abrir();
    escolher('Meia sombra');

    expect(destino().searchParams.get('light')).toBe('MEIA_SOMBRA');
  });

  it('preserva os filtros que já estavam escolhidos', () => {
    comFiltros('light=SOMBRA');
    abrir();
    escolher('Fácil de cuidar');

    const p = destino().searchParams;
    expect(p.get('light')).toBe('SOMBRA');
    expect(p.get('difficulty')).toBe('FACIL');
  });

  it('volta para a primeira página ao trocar de filtro', () => {
    // Continuar na página 3 de um resultado que agora tem uma página só
    // mostraria a vitrine vazia, e pareceria que não existe planta assim.
    comFiltros('page=3&light=SOMBRA');
    abrir();
    escolher('Sol pleno');

    expect(destino().searchParams.has('page')).toBe(false);
  });

  it('clicar na opção já escolhida desmarca', () => {
    comFiltros('light=SOMBRA');
    abrir();
    escolher('Sombra');

    expect(destino().searchParams.has('light')).toBe(false);
  });

  it('marca visualmente a opção escolhida', () => {
    comFiltros('light=SOL_PLENO');
    abrir();

    expect(screen.getAllByRole('button', { name: 'Sol pleno' })[0]).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getAllByRole('button', { name: 'Sombra' })[0]).toHaveAttribute('aria-pressed', 'false');
  });
});

describe('marcadores', () => {
  it('liga o filtro de segurança para animais', () => {
    comFiltros('');
    abrir();
    fireEvent.click(screen.getAllByLabelText(/Segura para cães e gatos/)[0]);

    expect(destino().searchParams.get('petSafe')).toBe('true');
  });

  it('desligar remove o parâmetro em vez de mandar false', () => {
    // petSafe=false devolveria só as tóxicas, o oposto do que a pessoa quer.
    comFiltros('petSafe=true');
    abrir();
    fireEvent.click(screen.getAllByLabelText(/Segura para cães e gatos/)[0]);

    expect(destino().searchParams.has('petSafe')).toBe(false);
  });

  it('reflete o que veio da URL', () => {
    comFiltros('petSafe=true&onlyAvailable=true');
    abrir();

    expect(screen.getAllByLabelText(/Segura para cães e gatos/)[0]).toBeChecked();
    expect(screen.getAllByLabelText(/Só as disponíveis/)[0]).toBeChecked();
  });
});

describe('faixa de preço', () => {
  const controle = () => screen.getAllByRole('slider', { name: /preço máximo/i })[0];

  it('vai do mais barato ao mais caro do estoque', () => {
    // Os extremos saem do catálogo, e não de valores escolhidos a dedo: numa
    // loja onde tudo custa menos de R$ 60, um teto de R$ 200 não serve.
    comFiltros('');
    abrir();

    expect(controle()).toHaveAttribute('min', '20');
    expect(controle()).toHaveAttribute('max', '120');
  });

  it('só muda a URL quando a pessoa solta o controle', () => {
    // Navegar a cada pixel arrastado dispararia uma consulta por quadro de
    // animação, e a lista piscaria sem parar.
    comFiltros('');
    abrir();
    fireEvent.change(controle(), { target: { value: '60' } });
    expect(empurrar).not.toHaveBeenCalled();

    fireEvent.mouseUp(controle(), { target: { value: '60' } });
    expect(destino().searchParams.get('maxPrice')).toBe('60');
  });

  it('arrastar até o fim significa "tanto faz", e não "até o mais caro"', () => {
    comFiltros('maxPrice=60');
    abrir();
    fireEvent.change(controle(), { target: { value: '120' } });
    fireEvent.mouseUp(controle(), { target: { value: '120' } });

    expect(destino().searchParams.has('maxPrice')).toBe(false);
  });

  it('o chip do preço fala a mesma língua do painel', () => {
    // O chip dizia "Até R$ 60" e o painel logo abaixo, "Até R$ 60,00". Dois
    // jeitos de escrever o mesmo número, um ao lado do outro.
    comFiltros('maxPrice=60');

    expect(
      screen.getByRole('button', { name: /Remover filtro Até R\$\s?60,00/ }),
    ).toBeInTheDocument();
  });

  it('preço inválido na URL não vira filtro nem chip', () => {
    // Com `?maxPrice=abc` a barra anunciava "Até R$ abc" e a API ignorava o
    // parâmetro: a tela dizia estar filtrando o que não estava filtrado.
    comFiltros('maxPrice=abc');

    expect(screen.queryByRole('button', { name: /Remover filtro/ })).toBeNull();
    expect(botaoFiltrar()).not.toHaveTextContent(/\d/);
  });

  it('preço inválido não chega como NaN ao leitor de tela', () => {
    // O texto visível escapava, mas o `aria-valuetext` do controle anunciava
    // "Até R$ NaN" para quem usa leitor de tela.
    comFiltros('maxPrice=abc');
    abrir();

    expect(controle().getAttribute('aria-valuetext')).toBe('Qualquer preço');
  });

  it('preço negativo é tratado como ausente', () => {
    comFiltros('maxPrice=-5');

    expect(screen.queryByRole('button', { name: /Remover filtro/ })).toBeNull();
  });

  it('mostra o valor em reais, e não o número cru', () => {
    comFiltros('maxPrice=45');
    abrir();

    expect(screen.getAllByText(/Até R\$\s?45,00/).length).toBeGreaterThan(0);
  });
});

describe('opções oferecidas', () => {
  it('não oferece "Dentro ou fora" como filtro de ambiente', () => {
    // Filtrar por AMBOS não faz sentido para quem compra: quem quer planta de
    // dentro escolhe "Dentro de casa", e o backend já inclui as que servem para
    // os dois. Oferecer a opção sugeriria um terceiro grupo que não existe.
    comFiltros('');
    abrir();

    expect(screen.queryByRole('button', { name: 'Dentro ou fora' })).toBeNull();
    expect(screen.getAllByRole('button', { name: 'Dentro de casa' }).length).toBeGreaterThan(0);
  });
});
