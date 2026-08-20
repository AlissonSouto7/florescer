# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), versionamento em [SemVer](https://semver.org/lang/pt-BR/).

## Não lançado

### Adicionado

- **Vitrine e painel em Next.js**, substituindo o frontend estático. A vitrine renderiza no servidor, então o HTML já chega com as plantas e cada uma é indexável.
- **Botão de compra pelo WhatsApp**, com a planta e o preço na mensagem. Não aparece em planta sem estoque.
- **Painel da vendedora**: cadastrar, editar e excluir pela tela, com prévia da foto e preço aceito com vírgula.
- **Sete campos que quem compra pergunta**: altura, luminosidade, rega, segurança para animais, ambiente, dificuldade e se o vaso está incluso.
- **Filtros na vitrine** por esses campos e por faixa de preço, resolvidos no banco. Planta marcada como `AMBOS` aparece nas buscas por interno e por externo.
- **Dados da loja editáveis pela vendedora** (`/admin/configuracoes`): WhatsApp que recebe os pedidos, cidade de entrega, Instagram e horário de atendimento. Saíram da variável de ambiente e do texto fixo no rodapé, porque trocar qualquer um deles exigia editar arquivo e reiniciar container.
- **Filtros refeitos para celular e para computador**: gaveta sobreposta no telefone, com as plantas logo abaixo da barra, e painel que encolhe no computador. Antes era uma coluna sempre aberta, que empurrava a primeira planta para fora da tela no celular.
- **Filtro por faixa de preço** e atalho de WhatsApp no próprio cartão da vitrine.
- **Erros de senha contados por conta**, com a senha certa passando mesmo com o contador estourado.
- **`Content-Security-Policy`** e `Permissions-Policy` no site.

### Corrigido

- Environment `production` recusava deploy disparado por tag, o que quebrava o CD da release.
- Imagens não carregavam: o Next 16 recusa otimizar imagem de host que resolve para IP privado. Passaram a ser servidas pelo domínio do frontend.
- Fundo escuro com texto escuro para quem usa o sistema em tema escuro.
- Painel pedia página maior que o teto da API e falhava ao listar.
- Abrir os filtros no computador tirava a barra de rolagem da página e deslocava o conteúdo 7px para o lado.
- A gaveta de filtros se dizia modal para o leitor de tela, mas o foco continuava fora dela e o Tab passeava pela vitrine atrás.
- `?page=999` mostrava "nenhuma planta com esses filtros" sem filtro nenhum aplicado, e sem caminho de volta.
- Preço inválido na URL virava `NaN`: a barra anunciava um filtro que não estava aplicado, e o leitor de tela recebia "Até R$ NaN".
- O título da planta repetia a marca na aba, no Google e na prévia do link.
- Dez senhas erradas de um desconhecido trancavam a vendedora fora do painel por um minuto.

### Segurança

- Documentação viva da API (`/swagger` e `/v3/api-docs`) **desligada por padrão**, ligada explicitamente só em desenvolvimento.
- APIs e bancos publicados apenas em `127.0.0.1` no compose de desenvolvimento.
- Cabeçalho `X-Powered-By` removido.
- Telefone, cidade e horário reais saíram do repositório: testes, exemplos do Swagger e documentação usam valores de exemplo.

### Removido

- Frontend estático (`florescer-frontend/`), substituído pelo Next. Junto com ele saiu a tela de registro: o comprador chega à vendedora pelo WhatsApp sem criar conta, e a conta dela vem de configuração. O endpoint de registro continua existindo e testado.

## [0.1.0] - 2026-08-11

Primeira versão que funciona de ponta a ponta e pode ser publicada. O código existia antes; o que esta versão traz é ele funcionando, seguro, testado e documentado.

### Segurança

- **Chave privada RSA removida do repositório e do histórico.** O par que assinava todos os tokens estava versionado. Rotacionado, purgado com `git filter-repo`, e agora carregado por configuração.
- **Conta administrativa deixou de ser fixa no código.** Era `admin@florescer.com`/`admin123`, impressa no stdout a cada boot. Agora vem do ambiente, desligada por padrão, e a senha nunca é registrada.
- **Path traversal no upload.** O nome do arquivo enviado era usado como está, com criação de diretório no caminho derivado. O nome passou a ser descartado por inteiro.
- **XSS armazenado pelo upload.** O tipo era aceito pelo `Content-Type` declarado e a extensão preservada. Agora o tipo vem dos bytes reais do arquivo.
- **Enumeração de contas.** As respostas de erro devolviam o e-mail informado. Resposta genérica, endereço fora do corpo.
- **Dado pessoal no log.** E-mail em texto puro virou pseudônimo estável (SHA-256 com salt), que mantém a auditoria sem gravar o endereço.
- **Rate limit** em login e registro, antes da consulta ao banco e da verificação de BCrypt.
- **Unicidade de e-mail no banco**, fechando a corrida entre a verificação e o insert.
- **Política de senha** de 12 a 64 caracteres sem exigir símbolo, seguindo o NIST 800-63B. O login deixou de validar formato.
- **CORS** configurável nos dois serviços, sem curinga.
- **Bean Validation** passou a de fato executar no product-service, onde era declarada e nunca rodava.
- **Actions do pipeline fixadas por SHA**, não por tag móvel.

### Adicionado

- **JWKS** em `/.well-known/jwks.json`, com `kid` derivado do thumbprint da chave. Trocar a chave deixou de exigir redeploy do serviço que valida.
- **Endpoint de saúde** com liveness e readiness separados, exposto sem detalhe interno.
- **Log estruturado**: texto em desenvolvimento, JSON por linha em produção.
- **Correlation id** atravessando os serviços, aceito do cliente ou gerado, validado antes de entrar no log.
- **Auditoria de escrita** no product-service, que antes não registrava nada.
- **`docker-compose.yml` na raiz** subindo a stack inteira, com healthcheck, volumes nomeados e portas configuráveis.
- **README** na raiz e por serviço, **`docs/features/`** com os achados de segurança por área, e **`docs/workflow/`** com o processo.
- **Migrations com Flyway** nos dois serviços, com `ddl-auto: validate`.

### Corrigido

- **O sistema não funcionava ponta a ponta.** O frontend chamava produtos na porta do auth, lia `data.token` quando a API devolve `accessToken`, e lia `imagePath` quando o DTO expõe `imageUrl`. A vitrine nunca carregava.
- **Imagens inacessíveis**: a URL gerada apontava para um caminho diferente do que o handler servia, e nenhum dos dois estava liberado para leitura anônima.
- **Erro de cliente virando 500** nos dois serviços: corpo malformado, método errado, rota inexistente e arquivo ausente respondiam erro de servidor.
- **Preço em `Double`**, que não representa decimal exatamente, trocado por `BigDecimal` com escala fixa.
- **I/O de disco dentro da transação**, que deixava arquivo órfão ou apagava a imagem de produto que sobreviveu.
- **`toString` da entidade `User`** expunha o hash da senha.
- **Paginação sem teto** e ordenação por campo arbitrário numa rota pública.
- **PATCH exigia imagem**, contrariando a própria documentação.
- **Autenticação duplicada** no auth-service: o filtro custava um `SELECT` por requisição, registrava e-mail em três pontos, transformava token válido de conta ausente em erro de servidor e respondia texto puro fora do contrato JSON.
- **Upload falhava dentro do container** por permissão do volume, defeito que nenhum teste pegava porque os testes gravam no host.

### Testes

De 2 testes que só verificavam se o contexto sobe, para **146**: 62 no auth-service e 84 no product-service, todos contra MySQL e PostgreSQL reais em container.

- Matriz de autorização percorrendo cada endpoint contra anônimo, BASIC e ADMIN.
- Corrida no registro provocada de forma determinística, sem `sleep`.
- Rotação de chave via JWKS provada sem reiniciar o serviço.
- Cobertura com piso obrigatório no CI: 80% de linha no auth, 75% no product, 55% de ramo nos dois.

### Processo

- GitFlow com `main` e `develop`, proteção de branch e status checks obrigatórios.
- CI com build, testes, piso de cobertura, varredura de segredo (Gitleaks) e análise estática (CodeQL).
- CD publicando imagem por ambiente: `develop` para dev, `release/*` para staging, tag para produção.
- Kanban com issues, labels e milestones.

### Conhecido e em aberto

- Sem `aud` no token: emitir e validar exige ordem de deploy, e merece entrega própria.
- Uma chave ativa por vez: o `kid` já permite convivência, mas o serviço carrega um par só.
- Sem refresh token nem revogação: um token vazado vale até expirar, em uma hora.
- Uploads servidos pela mesma origem da API (issue #23).
- Log injection potencial no product-service, não medido (issue #52).
