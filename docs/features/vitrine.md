# Vitrine e painel da vendedora

A interface: onde quem compra escolhe a planta e onde quem vende cadastra.

**Onde fica**: `florescer-web`, porta 3000, Next.js 16 com React 19. Também é o proxy: o navegador fala só com este domínio, e as chamadas para `/api/**` e `/uploads/**` são repassadas às APIs pela rede interna.
**Status**: funcional.
**Última revisão**: 20/08/2026.

## Telas

| Rota | Quem acessa | O que faz |
|---|---|---|
| `/` | qualquer um | vitrine com filtros |
| `/planta/[id]` | qualquer um | detalhe da planta e botão de WhatsApp |
| `/login` | qualquer um | entrada da vendedora |
| `/admin` | ADMIN | lista das plantas, com editar e excluir |
| `/admin/nova` | ADMIN | cadastro |
| `/admin/[id]` | ADMIN | edição |
| `/admin/configuracoes` | ADMIN | dados da loja: WhatsApp, cidade, Instagram e horário |

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

## O navegador fala só com este domínio

As chamadas do navegador vão para `/api/product`, `/api/auth` e `/uploads`, no domínio do próprio site, e o servidor do Next repassa para os serviços. Três motivos, em ordem de importância:

1. **As APIs não precisam ser públicas.** Em produção, só a porta do frontend é publicada. Uma porta exposta na internet em vez de cinco, contando os bancos.
2. **A mesma imagem serve todos os ambientes.** O endereço público embutido no build (o que `NEXT_PUBLIC_*` faz) obrigaria a reconstruir por ambiente, e a imagem testada em staging deixaria de ser a que vai para produção.
3. **CORS deixa de existir para o navegador**, porque tudo vem da mesma origem.

Duas armadilhas que só aparecem rodando:

**O destino do rewrite é resolvido no build**, não em runtime. `PRODUCT_API` e `AUTH_API` precisam ir como `ARG` no Dockerfile; passá-las só no `environment` do container não tem efeito nenhum, e a falha é silenciosa.

**O rewrite repassa o header `Origin` do navegador**, e o Spring aplica a política de CORS ao ver esse header, mesmo numa requisição que para o navegador é same-origin. Com `CORS_ALLOWED_ORIGINS` vazio, todo login responde `403 Invalid CORS request`. Por isso o compose de produção declara o domínio público nos dois serviços, embora nenhum deles seja alcançado pelo navegador.

## Ser encontrado no Google

A vitrine é renderizada no servidor justamente para o buscador conseguir ler as plantas. Isso sozinho não basta: sem dizer o que existe e o que pode ser visitado, metade do esforço se perde.

| Arquivo | O que resolve |
|---|---|
| `app/sitemap.ts` | entrega a lista completa das plantas disponíveis. Sem ela, o buscador só acha uma planta se alcançar o link dela partindo da vitrine, e o que caiu para a segunda página pode nunca ser encontrado |
| `app/robots.ts` | libera a vitrine e mantém `/admin` e `/login` fora da busca. "florescer login" não é resultado útil, e cada visita de robô a uma página com sessão gasta o rastreamento que deveria ir para as plantas |
| `components/DadosEstruturados.tsx` | descreve cada planta como `Product` do schema.org, o que faz o Google mostrar **foto, preço e disponibilidade no próprio resultado**, em vez de um link seco |
| `app/not-found.tsx` | link antigo deixa de cair na tela padrão do Next, em inglês e sem volta. A maioria dos 404 aqui é link de planta vendida, compartilhado semanas antes, e essa pessoa quer plantas |

Nada disso é proteção: quem impede o acesso ao painel é o backend, que recusa requisição sem token de ADMIN. O `noindex` das áreas internas é higiene de busca.

### Três armadilhas encontradas ao fazer

**O sitemap era gerado no build, e saía vazio.** O Next monta rotas estáticas durante o `next build`, quando a API não está de pé, e no Docker ela nem existe ainda, porque a rede sobe depois. A chamada falhava, o `catch` devolvia só a vitrine, e o arquivo ficava **sem nenhuma planta** até o primeiro revalidate. Medido: 1 URL logo após subir, 8 depois de expirar. Se o buscador pedisse nessa janela, concluiria que a loja tem uma página só. Resolvido com `dynamic = 'force-dynamic'`.

**O `size` da API tem teto de 50.** `PageableFactory.MAX_PAGE_SIZE` recusa mais que isso com `400`, e não devolve uma lista cortada. A recusa é boa: um sitemap com metade das plantas passaria despercebido. Por isso o sitemap pagina em vez de pedir tudo de uma vez.

**A imagem de compartilhamento apontava para o host interno.** O `openGraph` usava a `imageUrl` que a API devolve, que traz o host de quem chamou. Em produção seria `product-service:8081`, e quem recebesse o link no WhatsApp não veria foto. Agora vai o caminho, resolvido pelo `metadataBase` contra o endereço público.

## Segurança

### Achados corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| V-3 | médio | **nenhuma imagem carregava**, sem erro visível e com `naturalWidth = 0` | as fotos passaram a ser servidas pelo domínio do próprio frontend, por rewrite |
| V-6 | médio | falha de infraestrutura aparecia na tela como **"E-mail ou senha incorretos"**. Um `403` de CORS levava a vendedora a concluir que errou a senha, tentar de novo e trocar a senha, sem nunca entrar | só `401` e `404` recebem a mensagem genérica, que existe para não revelar se a conta existe. Qualquer outro status diz que é falha do sistema e mostra o código |

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
- **O número de WhatsApp vem do banco**, editado pela vendedora, nunca do código: [dados-da-loja.md](dados-da-loja.md).
- **Nenhum `dangerouslySetInnerHTML`.** Todo texto vindo da API é renderizado como texto pelo React.

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| V-1 | médio | o token fica em `sessionStorage`, alcançável por XSS | `sessionStorage` some ao fechar a aba, o que reduz a janela, mas a correção real é cookie `HttpOnly`, e isso exige o backend emitir o cookie |
| V-2 | baixo | não há proteção de rota no servidor: `/admin` verifica a sessão no navegador | o backend recusa qualquer requisição sem token ADMIN, então o dado está protegido; o que falta é a tela não piscar antes de redirecionar |

## Testes

249 testes em Vitest com Testing Library, rodando em jsdom. Medido em 20/08/2026: `lib/` com 100% de linhas e 92% de ramos, `components/` com 97,9% de linhas e 93,6% de ramos.

```bash
cd florescer-web
npm test              # a suíte
npm run test:coverage # com o gate de cobertura
```

O gate tem piso por pasta, e não um piso global: `lib/**` exige 95% de linhas e 85% de ramos, `components/**` exige 93% e 88%. As páginas de `app/` entram no relatório com zero, de propósito, para o número não parecer melhor do que é.

| Arquivo | Testes | Risco que protege |
|---|---|---|
| `Filtros` | 32 | filtro que não chega à URL; página antiga preservada ao trocar de filtro; `petSafe=false` devolvendo só as tóxicas; painel aberto travando a rolagem da página atrás dele |
| `lib/api` | 29 | parâmetro que deixa de ser enviado; 404 virando tela de erro; parte `product` sem `application/json`; login revelando se a conta existe; **URL com host, que reabriria as APIs para a internet**; falha de sistema disfarçada de senha errada |
| `Select` | 22 | lista que não abre pelo teclado; `aria-activedescendant` no elemento errado, que faz o leitor de tela não anunciar a troca; valor que não entra no `FormData` |
| `CardPlanta` | 17 | foto apontando para o host interno; selo de esgotada ausente; "null" na tela em planta antiga |
| `lib/sessao` | 17 | payload base64url quebrando o `atob` e gerando laço de login; `ADMINISTRADOR` passando por `ADMIN`; token indo para `localStorage` |
| `FormularioPlanta` | 16 | preço com vírgula virando NaN; checkbox desmarcado sumindo do payload; cadastro sem foto indo à API; duplo clique cadastrando duas vezes |
| `FormularioDaLoja` | 14 | ver [dados-da-loja.md](dados-da-loja.md) |
| `FiltroDePreco` | 14 | URL mudando a cada pixel arrastado; arrastar até o topo deixando de significar "sem teto" |
| `lib/whatsapp` | 14 | mensagem corrompida por acento, `&` ou `#`; botão em planta sem estoque; link para `wa.me` sem número |
| `lib/rotulos` | 12 | URL da imagem voltando absoluta (issue #89); enum vazando para a tela |
| `lib/loja` | 12 | API fora do ar derrubando a vitrine inteira por causa do rodapé; link do Instagram montado com `@` |
| `BotaoWhatsApp` | 10 | botão em planta sem estoque; loja sem número mandando o visitante para uma página de erro do WhatsApp; aba nova com acesso a esta janela |
| `DadosEstruturados` | 10 | preço em JSON-LD no formato que a pessoa lê, e não no que o Google aceita; `<` no nome da planta fechando o `<script>` |
| `app/sitemap` | 10 | sitemap gerado vazio no build; teto de 50 por página cortando o catálogo em silêncio |
| `MenuDaVendedora` | 8 | menu aparecendo para quem não tem sessão; sessão vencida ainda mostrando o painel |
| `lib/site` | 7 | endereço público caindo para `localhost` e tirando a foto da prévia do link |
| `app/robots` | 5 | painel e login indo parar na busca |

### Prova de que os testes não são vacuosos

28 mutações aplicadas ao código de produção, uma por vez, com a suíte rodando entre cada uma. **As 28 foram acusadas**, cada uma pelo teste que deveria pegá-la. A que devolve o navegador a chamar a API por host, o que reabriria as portas para a internet, é pega por 8 testes de uma vez.

A primeira rodada teve 22 de 23. A que escapou removia a conversão base64url de `papeis()`, e o teste dessa conversão exercitava só `expirado()`: a conversão está escrita duas vezes, uma em cada função, e o teste cobria uma só. O caso faltante virou teste, e a mutação passou a ser acusada.

O script exige baseline verde antes de começar. Sem isso, "a suíte falhou" não provaria nada: ela já podia estar falhando antes.

Uma segunda rodada, em 20/08/2026, cobriu o código dos dados da loja, que não existia na primeira: 18 mutações, 16 acusadas. Das duas que passaram, uma é mutante equivalente (trocar o `return LOJA_VAZIA` por um `throw` cai no `catch` logo abaixo e produz o mesmo resultado, então não há comportamento novo para um teste observar) e **a outra era buraco real**: nada garantia que o rodapé não voltasse a guardar em cache, que foi justamente o defeito C-3 de [dados-da-loja.md](dados-da-loja.md). Virou teste, com vermelho antes e verde depois.

### Verificado com a stack de produção

Em 19/08/2026, com `docker-compose.prod.yml`, as portas 8080, 8081, 3306 e 5432 **fechadas** e apenas o frontend publicado:

| Verificação | Resultado |
|---|---|
| login pelo domínio do site | 200 |
| cadastro com foto (multipart + `Authorization` pelo proxy) | 201, acento preservado |
| foto servida pelo domínio do frontend | 200, PNG íntegro |
| planta na vitrine renderizada no servidor | presente no HTML |
| botão de WhatsApp com acento codificado | `vi%C3%A7osa` |
| edição pelo proxy | 204 |
| cadastro sem token e com token inválido | 401 nos dois |

No navegador, a vendedora fez login, cadastrou uma planta com foto e a viu na vitrine: `45,90` chegou como `R$ 45,90`, o acento sobreviveu, e a foto carregou de fato (`naturalWidth` 315, não zero).

### O que NÃO está coberto

- **As páginas de `app/`**: são componentes de servidor que buscam da API e montam a tela. Testá-las em jsdom exigiria simular o runtime do Next inteiro.
- **Ponta a ponta com navegador**: cadastrar no painel e ver a planta aparecer na vitrine continua sendo verificação manual.
- **O que jsdom não enxerga**: layout, contraste, e se a imagem carrega de fato. jsdom não baixa imagem nem calcula estilo, então `naturalWidth > 0` só é verificável no navegador.
- **Responsividade e teclado**: nenhuma verificação automatizada de foco visível ou de navegação por Tab.

## Como verificar em produção

```bash
# A vitrine responde e traz plantas no HTML (e não só depois do JavaScript)?
curl -s http://HOST:3000 | grep -c "planta/"

# O filtro chega ao backend?
curl -s "http://HOST:8081/v1/product?petSafe=true&size=5" | jq '.totalElements'
```

No navegador, o que confirma que o essencial funciona: abrir a vitrine, marcar "segura para cães e gatos", ver a lista diminuir e a URL mudar, abrir uma planta e conferir que o link do WhatsApp traz nome e preço na mensagem.

## Dívida conhecida

- Sem teste de ponta a ponta com navegador (acima).
- Sem busca por texto.
- Sem `minPrice`: só o teto.
- A edição envia todos os campos, mesmo os não alterados. Funciona, porque o PATCH aplica o que veio, mas dois cadastros simultâneos na mesma planta sobrescrevem um ao outro.

## Histórico

| Data | O que mudou |
|---|---|
| 20/08/2026 | rodapé alimentado pelos dados da loja; filtros refeitos para celular (gaveta) e para PC (painel que encolhe); `Select` próprio no lugar do nativo; filtro por preço; atalho de WhatsApp no cartão; catálogo com plantas e fotos reais |
| 19/08/2026 | o navegador passou a falar só com o domínio do site; as APIs saíram da internet |
| 19/08/2026 | 112 testes automatizados, gate de cobertura e job próprio no CI |
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
