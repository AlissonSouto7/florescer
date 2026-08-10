document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('product-list')) carregarProdutos();
  if (document.getElementById('login-form')) configurarLogin();
  if (document.getElementById('register-form')) configurarCadastro();
});

/**
 * Envia JSON e devolve a resposta já interpretada.
 *
 * Concentra o tratamento de erro num lugar só: antes, cada tela repetia o mesmo
 * `.then(res => res.ok ? res.json() : Promise.reject())`, que descartava a
 * mensagem vinda do servidor e mostrava sempre o mesmo texto genérico.
 */
async function postJson(url, corpo) {
  const resposta = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(corpo),
  });

  const dados = await resposta.json().catch(() => null);

  if (!resposta.ok) {
    const erro = new Error((dados && (dados.details || dados.error)) || 'Não foi possível concluir.');
    erro.status = resposta.status;
    throw erro;
  }

  return dados;
}

async function carregarProdutos() {
  const container = document.getElementById('product-list');
  container.textContent = 'Carregando produtos...';

  try {
    const resposta = await fetch(`${API.product}/v1/product`);
    if (!resposta.ok) throw new Error('resposta não OK');

    const pagina = await resposta.json();
    container.textContent = '';

    if (!pagina.content.length) {
      container.textContent = 'Nenhum produto disponível no momento.';
      return;
    }

    pagina.content.forEach((produto) => container.appendChild(criarCard(produto)));
  } catch {
    container.textContent = 'Não foi possível carregar os produtos.';
  }
}

/**
 * Monta o cartão com createElement e textContent.
 *
 * A versão anterior usava innerHTML interpolando nome e descrição vindos da API.
 * Qualquer texto salvo no banco com marcação HTML seria executado no navegador
 * de quem visita. textContent escreve texto como texto, sempre.
 */
function criarCard(produto) {
  const card = document.createElement('div');
  card.className = 'product-card';

  const img = document.createElement('img');
  // O DTO expõe imageUrl. O código antigo lia imagePath, campo que não existe,
  // então toda imagem virava src="undefined".
  img.src = produto.imageUrl ?? '';
  img.alt = produto.name;
  img.loading = 'lazy';

  const nome = document.createElement('h3');
  nome.textContent = produto.name;

  const descricao = document.createElement('p');
  descricao.textContent = produto.description;

  const preco = document.createElement('p');
  const valor = document.createElement('strong');
  valor.textContent = formatarPreco(produto.price);
  preco.appendChild(valor);

  card.append(img, nome, descricao, preco);
  return card;
}

function formatarPreco(valor) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor ?? 0);
}

function configurarLogin() {
  const form = document.getElementById('login-form');
  const erro = document.getElementById('login-error');

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    erro.textContent = '';

    const botao = form.querySelector('button[type="submit"]');
    botao.disabled = true;

    try {
      // Campos por name, e não pela ordem em que aparecem: a versão anterior
      // usava querySelectorAll('input') e desestruturava por posição, então
      // reordenar o HTML trocaria e-mail por senha em silêncio.
      const dados = await postJson(`${API.auth}/v1/auth/login`, {
        email: form.elements.email.value,
        password: form.elements.password.value,
      });

      // A API devolve accessToken. O código antigo lia data.token, campo
      // inexistente, e gravava a string "undefined" como se fosse o token.
      localStorage.setItem('accessToken', dados.accessToken);
      window.location.href = 'index.html';
    } catch (e) {
      erro.textContent = e.status === 401 ? 'E-mail ou senha incorretos.' : e.message;
    } finally {
      botao.disabled = false;
    }
  });
}

function configurarCadastro() {
  const form = document.getElementById('register-form');
  const erro = document.getElementById('register-error');

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    erro.textContent = '';

    const botao = form.querySelector('button[type="submit"]');
    botao.disabled = true;

    try {
      await postJson(`${API.auth}/v1/auth/register`, {
        name: form.elements.name.value,
        email: form.elements.email.value,
        password: form.elements.password.value,
      });

      window.location.href = 'login.html?cadastro=ok';
    } catch (e) {
      erro.textContent = e.message;
    } finally {
      botao.disabled = false;
    }
  });
}
