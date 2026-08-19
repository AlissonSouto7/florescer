# Vitrine e painel da vendedora

A interface: onde quem compra escolhe a planta e onde quem vende cadastra.

**Onde fica**: `florescer-web`, porta 3000, Next.js 16 com React 19.
**Status**: funcional.
**Última revisão**: 11/08/2026.

## Telas

| Rota | Quem acessa | O que faz |
|---|---|---|
| `/` | qualquer um | vitrine com filtros |
| `/planta/[id]` | qualquer um | detalhe da planta e botão de WhatsApp |
| `/login` | qualquer um | entrada da vendedora |
| `/admin` | ADMIN | lista das plantas, com editar e excluir |
| `/admin/nova` | ADMIN | cadastro |
| `/admin/[id]` | ADMIN | edição |

## Decisões e por quê

**A vitrine é renderizada no servidor.** O HTML chega com as plantas dentro, então o Google indexa cada uma e quem abre no celular vê o conteúdo sem esperar o JavaScript baixar. Numa loja, aparecer na busca é o que traz o cliente.

**O filtro vive na URL, não em memória.** Escolher "segura para gatos" muda o endereço da página. Isso faz o filtro sobreviver ao recarregar, permite mandar o link já filtrado para alguém, e faz o botão voltar do navegador desfazer o filtro em vez de sair da vitrine.

**O filtro roda no banco.** Mandar o catálogo inteiro para o navegador filtrar funciona com dez plantas e piora a cada planta nova, gastando dados de quem está no celular.

**Trocar um filtro volta para a primeira página.** Continuar na página 3 de um resultado que agora tem uma página só mostraria a vitrine vazia sem explicação.

**O botão de WhatsApp leva a planta na mensagem.** Sem isso a vendedora recebe "olá" sem contexto e a conversa recomeça do zero. A mensagem passa por `encodeURIComponent`, senão acento e `&` no nome cortam o texto.

**Planta sem estoque não mostra o botão.** Recebe um aviso de indisponível. Um pedido que não pode ser atendido é pior que nenhum pedido.

**Segurança para animais aparece nos dois sentidos.** Quem tem gato precisa ver o aviso de "tóxica", não a ausência do "segura". No cartão só aparece quando é segura, porque um alerta em letra miúda assusta sem explicar; a página de detalhe diz com todas as letras.

**O formulário já vem preenchido nas opções mais comuns** (ambiente interno, fácil de cuidar, vai com vaso). Sete campos novos não podem virar sete decisões novas a cada planta.

**O preço aceita vírgula.** Quem digita `49,90` escreve como fala. Obrigar `49.90` gera erro silencioso.

**A foto tem prévia antes de salvar**, para conferir que é a planta certa. A URL de objeto é revogada ao trocar a foto, senão cada troca deixa a imagem anterior ocupando memória.

**A exclusão confirma dizendo o nome da planta.** "Tem certeza?" sozinho é fácil de clicar no automático, e `DELETE` não tem volta.

## Segurança

### Achados corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| V-3 | médio | **nenhuma imagem carregava**, sem erro visível e com `naturalWidth = 0` | as fotos passaram a ser servidas pelo domínio do próprio frontend, por rewrite |

A causa não é a aparente. O Next 16 **recusa otimizar imagem cujo host resolve para IP privado**, como proteção contra SSRF, e a única pista está no log do servidor:

```
upstream image http://localhost:18081/uploads/x.png
hostname resolved to private IP ["::1","127.0.0.1"]
If this is expected and you understand SSRF risk,
use images.dangerouslyAllowLocalIP = true to continue.
```

Tanto `product-service` (rede do Docker) quanto `localhost` são privados, então toda foto respondia 400. Do lado do navegador não há sintoma nenhum: o build passa, a página renderiza, a tag `img` existe no HTML. Verificar "a imagem está no DOM" teria dado verde.

**A opção `dangerouslyAllowLocalIP` existe e foi recusada**, porque reabre o buraco que a checagem fecha, num ponto onde a URL vem de dado do banco.

A saída foi um rewrite: `/uploads/*` no domínio do frontend é proxiado para o product-service. A imagem vira same-origin, o Next a trata como local, e quem busca do backend é o servidor do Next, para **um host fixo definido na configuração**, e não para qualquer endereço que a API devolver. De quebra, isso neutraliza a issue #89 no frontend, porque a URL que a API sugere deixa de importar: guardamos só o caminho.

Uma armadilha junto: o destino do rewrite é **congelado no build**, não lido em runtime. Passar `PRODUCT_API` só no `environment` do compose não tem efeito; ele precisa ir como `ARG` também, e o Dockerfile faz isso.

### Verificado e OK

- **A tela esconder o botão não é a proteção.** Quem protege é o `@PreAuthorize` no backend, que continua valendo para quem chamar a API direto. A interface esconde por conforto, não por segurança.
- **O `next/image` só carrega de host declarado** no `next.config.ts`. Sem isso, uma URL vinda da API viraria requisição feita pelo nosso servidor, que é caminho para SSRF.
- **Cabeçalhos de segurança** (`nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`) são enviados pelo próprio Next.
- **O número de WhatsApp vem de configuração**, nunca do código.
- **Nenhum `dangerouslySetInnerHTML`.** Todo texto vindo da API é renderizado como texto pelo React.

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| V-1 | médio | o token fica em `sessionStorage`, alcançável por XSS | `sessionStorage` some ao fechar a aba, o que reduz a janela, mas a correção real é cookie `HttpOnly`, e isso exige o backend emitir o cookie |
| V-2 | baixo | não há proteção de rota no servidor: `/admin` verifica a sessão no navegador | o backend recusa qualquer requisição sem token ADMIN, então o dado está protegido; o que falta é a tela não piscar antes de redirecionar |

## Testes

**Nenhum teste automatizado.** Esta é a lacuna real desta entrega.

O que foi verificado, e como:

| Verificação | Como |
|---|---|
| compila sem erro de tipo | `npm run build`, com TypeScript em modo estrito |
| vitrine, filtros, detalhe, login, cadastro | navegador, contra a stack do Compose |

O que deveria existir, e não existe: teste de componente para o `BotaoWhatsApp` (principalmente a codificação da mensagem com acento), teste do formulário validando o preço com vírgula, e um teste de ponta a ponta cobrindo cadastrar e ver na vitrine.

## Como verificar em produção

```bash
# A vitrine responde e traz plantas no HTML (e não só depois do JavaScript)?
curl -s http://HOST:3000 | grep -c "planta/"

# O filtro chega ao backend?
curl -s "http://HOST:8081/v1/product?petSafe=true&size=5" | jq '.totalElements'
```

No navegador, o que confirma que o essencial funciona: abrir a vitrine, marcar "segura para cães e gatos", ver a lista diminuir e a URL mudar, abrir uma planta e conferir que o link do WhatsApp traz nome e preço na mensagem.

## Dívida conhecida

- Sem testes automatizados (acima).
- Sem busca por texto.
- Sem `minPrice`: só o teto.
- A edição envia todos os campos, mesmo os não alterados. Funciona, porque o PATCH aplica o que veio, mas dois cadastros simultâneos na mesma planta sobrescrevem um ao outro.

## Histórico

| Data | O que mudou |
|---|---|
| 11/08/2026 | vitrine, filtros, detalhe, WhatsApp, login e painel da vendedora |

## Correções feitas na validação visual

| id | o que era | correção |
|---|---|---|
| V-4 | com o sistema em tema escuro, o fundo virava preto enquanto os textos seguiam nos tons escuros do layout: o título ficava cinza-escuro sobre preto | a loja passou a ter um visual único e claro |
| V-5 | foco do teclado invisível em parte dos elementos, depois que o CSS redefiniu cores | `:focus-visible` com contorno próprio |

O `globals.css` do template define `background` e `color` **no elemento `body`**, a partir de variáveis que trocam com `prefers-color-scheme: dark`. Regra de elemento vence classe utilitária na mesma especificidade, então as classes do layout eram ignoradas justamente para quem usa o sistema no escuro.

Não aparece em teste de API, nem no HTML, nem no build. Só olhando a tela, e só olhando a tela **com o sistema em tema escuro**.

O visual claro é escolha, não omissão: um catálogo existe para a foto aparecer, e fundo neutro deixa o verde se destacar. Também dá um contraste único e verificável (16,9:1, contra os 4,5:1 do WCAG AA), em vez de dois temas a conferir.

### V-6: o painel pedia mais do que a API permite

O painel listava as plantas com `size=100`. A API recusa acima de 50, e recusa **de propósito**: cortar em silêncio entregaria uma página diferente da pedida sem avisar (issue #12).

O resultado era a tela dizer "não foi possível carregar as plantas" com o catálogo cheio, e nada apontando para o tamanho da página como causa.

O limite funcionou como projetado. Se a API tivesse cortado para 50 em silêncio, o painel funcionaria por acaso, e o limite só apareceria quando a 51ª planta sumisse da lista.

Corrigido para 50, com aviso na tela quando houver mais plantas que o mostrado. **Quando o catálogo passar de 50, o painel precisa paginar de verdade**, e isso ainda não existe.
