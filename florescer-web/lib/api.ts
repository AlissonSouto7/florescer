/**
 * Acesso às duas APIs do Florescer.
 *
 * Os endereços vêm de variável de ambiente porque mudam por ambiente. Como a
 * vitrine é renderizada no servidor e o painel roda no navegador, existem duas
 * variáveis: a do servidor pode apontar para o nome do serviço dentro da rede do
 * Docker, enquanto a do navegador precisa de um endereço que o cliente alcance.
 */

const PRODUTOS_SERVIDOR = process.env.PRODUCT_API ?? 'http://localhost:8081';
const PRODUTOS_NAVEGADOR = process.env.NEXT_PUBLIC_PRODUCT_API ?? 'http://localhost:8081';
const AUTH_NAVEGADOR = process.env.NEXT_PUBLIC_AUTH_API ?? 'http://localhost:8080';

/** Do lado do servidor usa o endereço interno; no navegador, o público. */
const produtosBase = () => (typeof window === 'undefined' ? PRODUTOS_SERVIDOR : PRODUTOS_NAVEGADOR);

export type Luminosidade = 'SOL_PLENO' | 'MEIA_SOMBRA' | 'SOMBRA';
export type Rega = 'DIARIA' | 'DUAS_A_TRES_VEZES_SEMANA' | 'SEMANAL' | 'QUINZENAL' | 'MENSAL';
export type Ambiente = 'INTERNO' | 'EXTERNO' | 'AMBOS';
export type Dificuldade = 'FACIL' | 'MEDIO' | 'DIFICIL';
export type StatusPlanta = 'ATIVO' | 'INATIVO' | 'ESGOTADO';

export type Planta = {
  id: string;
  name: string;
  type: string;
  description: string;
  price: number;
  quantityStock: number;
  careRequirements: string;
  availability: boolean;
  status: StatusPlanta;
  imageUrl: string;
  // Nulos para plantas cadastradas antes destes campos existirem. A interface
  // precisa lidar com a ausência em vez de mostrar "null" na tela.
  heightCm: number | null;
  light: Luminosidade | null;
  watering: Rega | null;
  petSafe: boolean | null;
  environment: Ambiente | null;
  difficulty: Dificuldade | null;
  includesPot: boolean | null;
};

export type Pagina<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
};

export type Filtros = {
  light?: Luminosidade;
  petSafe?: boolean;
  environment?: Ambiente;
  difficulty?: Dificuldade;
  maxPrice?: number;
  onlyAvailable?: boolean;
  page?: number;
  size?: number;
};

function queryDe(filtros: Filtros): string {
  const params = new URLSearchParams();
  params.set('page', String(filtros.page ?? 0));
  params.set('size', String(filtros.size ?? 12));

  // Só o que foi escolhido vai para a URL: parâmetro vazio faria o backend
  // filtrar por nada e a URL da página ficaria ilegível de tão comprida.
  if (filtros.light) params.set('light', filtros.light);
  if (filtros.environment) params.set('environment', filtros.environment);
  if (filtros.difficulty) params.set('difficulty', filtros.difficulty);
  if (filtros.petSafe) params.set('petSafe', 'true');
  if (filtros.onlyAvailable) params.set('onlyAvailable', 'true');
  if (filtros.maxPrice) params.set('maxPrice', String(filtros.maxPrice));

  return params.toString();
}

export async function listarPlantas(filtros: Filtros = {}): Promise<Pagina<Planta>> {
  const resposta = await fetch(`${produtosBase()}/v1/product?${queryDe(filtros)}`, {
    // A vitrine muda quando a vendedora cadastra, não a cada segundo. Meio
    // minuto de cache corta a maioria das consultas ao banco sem que alguém
    // veja um catálogo desatualizado.
    next: { revalidate: 30 },
  });

  if (!resposta.ok) {
    throw new Error(`A vitrine não respondeu (${resposta.status})`);
  }
  return resposta.json();
}

export async function buscarPlanta(id: string): Promise<Planta | null> {
  const resposta = await fetch(`${produtosBase()}/v1/product/${id}`, { next: { revalidate: 30 } });

  // 404 é resposta legítima: quem seguiu um link antigo precisa ver "não
  // encontrada", não uma tela de erro.
  if (resposta.status === 404) return null;
  if (!resposta.ok) throw new Error(`Não foi possível carregar a planta (${resposta.status})`);

  return resposta.json();
}

export async function entrar(email: string, password: string): Promise<string> {
  const resposta = await fetch(`${AUTH_NAVEGADOR}/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!resposta.ok) {
    // A API responde a mesma coisa para conta inexistente e senha errada, de
    // propósito. A interface não pode ser mais específica que ela.
    throw new Error('E-mail ou senha incorretos.');
  }

  const { accessToken } = await resposta.json();
  return accessToken;
}

type RespostaErro = { error?: string; details?: Record<string, string> | string };

/** Extrai a mensagem por campo que a API devolve, para exibir ao lado do input. */
export async function errosDe(resposta: Response): Promise<Record<string, string>> {
  try {
    const corpo: RespostaErro = await resposta.json();
    if (corpo.details && typeof corpo.details === 'object') return corpo.details;
    return { _geral: typeof corpo.details === 'string' ? corpo.details : (corpo.error ?? 'Erro inesperado') };
  } catch {
    return { _geral: `Erro ${resposta.status}` };
  }
}

export async function salvarPlanta(
  dados: Record<string, unknown>,
  imagem: File | null,
  token: string,
): Promise<Response> {
  const form = new FormData();
  // O backend espera a parte "product" como JSON, não como campos soltos.
  form.append('product', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (imagem) form.append('image', imagem);

  return fetch(`${PRODUTOS_NAVEGADOR}/v1/product`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
}

export async function alterarPlanta(
  id: string,
  dados: Record<string, unknown>,
  imagem: File | null,
  token: string,
): Promise<Response> {
  const form = new FormData();
  form.append('product', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
  if (imagem) form.append('image', imagem);

  return fetch(`${PRODUTOS_NAVEGADOR}/v1/product/${id}`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
}

export async function excluirPlanta(id: string, token: string): Promise<Response> {
  return fetch(`${PRODUTOS_NAVEGADOR}/v1/product/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  });
}
