document.addEventListener('DOMContentLoaded', () => {
  const path = window.location.pathname;

  if (path.endsWith('index.html') || path.endsWith('/')) {
    carregarProdutos();
  } else if (path.endsWith('login.html')) {
    configurarLogin();
  } else if (path.endsWith('register.html')) {
    configurarCadastro();
  }
});

function carregarProdutos() {
  const container = document.getElementById('product-list');

  fetch('http://localhost:8080/v1/product')
    .then(res => res.json())
    .then(data => {
      data.content.forEach(produto => {
        const card = document.createElement('div');
        card.className = 'product-card';
        card.innerHTML = `
          <img src="${produto.imagePath}" alt="${produto.name}" />
          <h3>${produto.name}</h3>
          <p>${produto.description}</p>
          <p><strong>R$ ${produto.price.toFixed(2)}</strong></p>
        `;
        container.appendChild(card);
      });
    })
    .catch(() => {
      container.innerHTML = '<p>Erro ao carregar produtos.</p>';
    });
}

function configurarLogin() {
  const form = document.getElementById('login-form');
  const error = document.getElementById('login-error');

  form.addEventListener('submit', e => {
    e.preventDefault();
    const [email, password] = form.querySelectorAll('input');
    
    fetch('http://localhost:8080/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value, password: password.value })
    })
    .then(res => res.ok ? res.json() : Promise.reject())
    .then(data => {
      localStorage.setItem('token', data.token);
      window.location.href = 'index.html';
    })
    .catch(() => error.textContent = 'Login inválido.');
  });
}

function configurarCadastro() {
  const form = document.getElementById('register-form');
  const error = document.getElementById('register-error');

  form.addEventListener('submit', e => {
    e.preventDefault();
    const [name, email, password] = form.querySelectorAll('input');

    fetch('http://localhost:8080/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.value, email: email.value, password: password.value })
    })
    .then(res => res.ok ? res.json() : Promise.reject())
    .then(() => {
      alert('Cadastro realizado com sucesso!');
      window.location.href = 'login.html';
    })
    .catch(() => error.textContent = 'Erro ao registrar. Tente outro email.');
  });
}
