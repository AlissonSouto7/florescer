<div align="center">

# Florescer

**Catálogo online para uma loja de plantas.**

[![CI](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml)
[![CodeQL](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-000000)](https://nextjs.org)

</div>

Vitrine pública onde cada planta tem foto, descrição, cuidados e preço, e um painel onde quem vende mantém o catálogo sem depender de ninguém.

**O que este projeto é, em uma frase:** uma loja de bairro na internet, feita para uma pessoa que vende plantas em casa.

Isso decide o desenho inteiro, e vale dizer o que ele **não** tem: não há carrinho, não há cadastro de cliente, não há pagamento online. O visitante escolhe a planta e clica em comprar; o botão abre o WhatsApp da vendedora com o nome e o preço já escritos na mensagem, e a venda se fecha na conversa, como já se fechava antes. O sistema resolve a parte que faltava, que é ser encontrado e mostrar o que existe.

O que existe de verdade é um monorepo com três aplicações: dois serviços Spring Boot (identidade e catálogo, cada um com o seu banco) e um frontend Next.js que é, ao mesmo tempo, a loja e o proxy que mantém as APIs fora da internet.

## O que faz

**Para quem compra**

- Vitrine com foto, preço e o que decide a escolha: tamanho, luz que a planta aguenta, se convive com animais.
- Filtros por luminosidade, segurança para animais, ambiente, dificuldade e faixa de preço. O filtro fica na URL, então o link filtrado pode ser enviado a alguém.
- Página de cada planta com rega, ambiente, cuidados, e aviso claro quando a planta é tóxica.
- Botão de compra que abre o WhatsApp da vendedora com a planta e o preço já na mensagem.

**Para quem vende**

- Painel para cadastrar, editar e excluir pela tela, sem Swagger e sem `curl`.
- Preço aceito como se fala (`45,90`), foto com prévia antes de salvar, opções já marcadas nas respostas mais comuns.
- Exclusão confirma dizendo o nome da planta.
- Os dados da loja (WhatsApp que recebe os pedidos, cidade de entrega, Instagram e horário) são editados por ela na tela, e não em arquivo de configuração. Trocar o número não exige ninguém com acesso ao servidor.

## Arquitetura

Dois serviços independentes atrás do frontend. O navegador fala **só com o domínio do site**: o servidor do Next repassa as chamadas para as APIs pela rede interna, então em produção uma única porta fica exposta, e não três. O catálogo não consulta o serviço de identidade a cada requisição: valida a assinatura do token por conta própria, buscando a chave pública uma vez.

```
                          navegador
                              │
                              │  só esta porta é pública
                              ▼
                  ┌───────────────────────┐
                  │     florescer-web     │
                  │         :3000         │
                  │  vitrine, painel e    │
                  │  proxy para as APIs   │
                  └───────────┬───────────┘
                              │
        ── rede interna ──────┴──────────────────
              │                               │
          /api/auth                      /api/product
              │                          /uploads
              ▼                               ▼
   ┌──────────────────┐            ┌────────────────────┐
   │   auth-service   │            │  product-service   │
   │      :8080       │            │       :8081        │
   │                  │            │                    │
   │  assina o token  │            │  valida o token    │
   │  (chave privada) │            │  (chave pública)   │
   └────────┬─────────┘            └─────────┬──────────┘
            │                                │
            │   GET /.well-known/jwks.json   │
            │◄───────────────────────────────┘
            │                                │
            ▼                                ▼
      ┌──────────┐                     ┌──────────┐
      │  MySQL   │                     │ Postgres │
      └──────────┘                     └──────────┘
```

Três consequências práticas: o catálogo continua no ar mesmo se o serviço de identidade cair; trocar a chave de assinatura não exige redeploy dos dois lados; e, como nenhum endereço público fica embutido no build, a mesma imagem roda em qualquer ambiente.

Em desenvolvimento as APIs e os bancos são publicados apenas em `127.0.0.1`: dá para abrir o Swagger e conectar no banco da própria máquina, sem deixar nada disso ao alcance da rede local. Em produção ([`docker-compose.prod.yml`](docker-compose.prod.yml)) só a porta do frontend é publicada, e também em `127.0.0.1`, atrás do proxy que faz o HTTPS.

## Stack

**Backend** (dois serviços, mesma pilha)

| | |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5 |
| Segurança | Spring Security, OAuth2 Resource Server, JWT RS256 com JWKS |
| Persistência | Spring Data JPA, Flyway |
| Bancos | MySQL 8.4 (identidade), PostgreSQL 16 (catálogo) |
| Documentação da API | springdoc-openapi, **desligada por padrão** |
| Testes | JUnit 5, AssertJ, Testcontainers, JaCoCo |

**Frontend**

| | |
|---|---|
| Framework | Next.js 16.3 (App Router), React 19.2 |
| Linguagem | TypeScript 5 |
| Estilo | Tailwind CSS 4 |
| Testes | Vitest 4, Testing Library, jsdom |
| Papel duplo | é a vitrine **e** o proxy: nenhuma API precisa ser publicada |

**Infraestrutura**

| | |
|---|---|
| Execução | Docker e Docker Compose |
| Imagens | multi-stage, publicadas no GHCR para `amd64` e `arm64` |
| CI/CD | GitHub Actions, com gates obrigatórios por pull request |
| Operação | scripts de backup, restauração e smoke test em [`scripts/`](scripts/) |

## Começando

**Requisitos:** JDK 17 e Docker. Maven não é necessário, o wrapper está versionado.

```bash
# 1. Gerar o par de chaves (fora do repositório)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ~/florescer/app.key
openssl rsa -pubout -in ~/florescer/app.key -out ~/florescer/app.pub

# 2. Configurar
cp .env.example .env    # preencher conforme os comentários do arquivo

# 3. Subir
docker compose up --build
```

| | URL | Observação |
|---|---|---|
| Vitrine | http://localhost:3000 | |
| Painel da vendedora | http://localhost:3000/admin | não há link para cá em lugar nenhum do site, de propósito |
| Dados da loja | http://localhost:3000/admin/configuracoes | WhatsApp, cidade, Instagram e horário |
| API de identidade | http://localhost:8080/swagger | só com `SWAGGER_ENABLED=true`, que o compose de desenvolvimento já define |
| API de catálogo | http://localhost:8081/swagger | idem |

O painel exige uma conta ADMIN. Ela não é criada sozinha: preencha `ADMIN_ENABLED`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` no `.env` antes de subir. Sem isso o sistema sobe sem nenhuma conta, o que é melhor que subir com uma conta que todo mundo conhece.

O número de WhatsApp, a cidade de entrega, o Instagram e o horário de atendimento são editados pela
própria vendedora em `/admin/configuracoes`, e ficam no banco. Enquanto o número não estiver
preenchido, o botão de comprar não aparece, de propósito: um `wa.me` sem número abre uma página de erro.

Sem as chaves configuradas a aplicação não sobe. Isso é intencional: um valor padrão faria o sistema assinar tokens com uma chave conhecida por qualquer pessoa que leia o repositório.

### Problemas comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `jwt.private-key deve conter um PEM...` com o `.env` correto | variável de ambiente do sistema vence o `.env` no Compose | `echo $RSA_PRIVATE_KEY` e remova-a |
| `port is already allocated` | 3306, 5432, 8080, 8081 ou 3000 em uso | defina `AUTH_DB_PORT`, `PRODUCT_DB_PORT`, `AUTH_PORT`, `PRODUCT_PORT` ou `FRONTEND_PORT` no `.env` |
| `bind: An attempt was made to access a socket in a way forbidden by its access permissions` (Windows) | a porta caiu numa faixa que o Hyper-V reserva | `netsh interface ipv4 show excludedportrange protocol=tcp` lista as faixas; escolha uma porta fora delas |
| chave PEM cortada no meio | o Compose lê só até a primeira quebra de linha | `awk 'BEGIN{ORS="\\n"}1' app.key` |

## Testes

```bash
cd auth-service && ./mvnw verify     # ou product-service
cd florescer-web && npm run test:coverage
```

Cada serviço tem piso de cobertura obrigatório, verificado no CI.

Medido em 20/08/2026:

| | Testes | Linha | Ramo | Piso |
|---|---|---|---|---|
| auth-service | 80 | 92% | 77% | 80% / 55% |
| product-service | 125 | 88% | 70% | 75% / 55% |
| florescer-web | 274 | 99% | 93% | 93% / 85% |

Os serviços rodam contra MySQL e PostgreSQL reais via Testcontainers, então Docker precisa estar ativo. Banco em memória não é usado: SQL específico, tipo `NUMERIC` e comportamento de transação diferem justamente onde os defeitos aparecem.

Os números do `florescer-web` cobrem `lib/` e `components/`. As páginas de `app/` não têm teste, e aparecem com zero no relatório de propósito.

Teste que passa de primeira é suspeito, então as suítes são validadas quebrando o código de propósito, uma mutação por vez, e vendo se algum teste acusa.

Duas rodadas até agora no frontend: 28 mutações e 28 acusadas na primeira; 18 e 16 na segunda. Das duas sobreviventes, uma era mutante equivalente e **a outra era buraco de verdade**, que virou teste. No backend: 4 de 4 no resolvedor de cliente, 4 de 4 na contagem de erros de login, 2 de 2 no padrão da documentação da API e 6 de 6 nas regras dos dados da loja.

O registro dessas rodadas, incluindo o que escapou e por quê, está em [`docs/features/vitrine.md`](docs/features/vitrine.md) e [`docs/features/auth.md`](docs/features/auth.md).

## API

<details>
<summary><b>auth-service</b>: identidade</summary>

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/v1/auth/register` | público |
| `POST` | `/v1/auth/login` | público |
| `GET` | `/.well-known/jwks.json` | público |
| `GET` | `/actuator/health` | público |

</details>

<details>
<summary><b>product-service</b>: catálogo</summary>

| Método | Rota | Acesso |
|---|---|---|
| `GET` | `/v1/product` | público |
| `GET` | `/v1/product/{id}` | público |
| `POST` | `/v1/product` | ADMIN |
| `PATCH` | `/v1/product/{id}` | ADMIN |
| `DELETE` | `/v1/product/{id}` | ADMIN |
| `GET` | `/v1/settings` | público |
| `PUT` | `/v1/settings` | ADMIN |
| `GET` | `/uploads/**` | público |

</details>

A documentação viva (`/swagger` e `/v3/api-docs`) **não sobe por padrão**: ela lista cada rota, cada campo e quem precisa de token, o que é o mapa do sistema para quem procura por onde entrar. O compose de desenvolvimento a liga explicitamente com `SWAGGER_ENABLED`.

## Segurança

Decisões que valem conhecer antes de mexer no código.

- Chaves RSA nunca no repositório; carregadas por configuração, sem valor padrão.
- Upload identifica o tipo pelos **bytes** do arquivo, não pelo `Content-Type` declarado, e descarta o nome original.
- Log registra pseudônimo, nunca e-mail.
- Respostas de erro não revelam se uma conta existe.
- Unicidade de e-mail garantida por constraint, não por verificação prévia.
- Erros de senha são contados **por conta**, e a senha certa passa mesmo com o contador estourado: adivinhar senha fica caro sem que errar a senha de alguém vire uma forma de trancar essa pessoa.
- O site manda `Content-Security-Policy`, `nosniff`, `X-Frame-Options`, `Referrer-Policy` e `Permissions-Policy`, e não anuncia a versão do framework.
- Nenhum dado pessoal no repositório: telefone, cidade e horário são exemplos nos testes e na documentação, e os valores reais vivem só no banco.

Os achados de cada área estão em [`docs/features/`](docs/features/), separados em corrigidos, **abertos** (com o motivo de continuarem abertos) e **verificado e OK** (o que já foi investigado e não está quebrado, para ninguém gastar uma tarde reinvestigando).

O que está aberto hoje, resumido: o token da vendedora fica no navegador em `sessionStorage` (a correção real é cookie `HttpOnly`); não há HTTPS nem HSTS, porque ainda não há domínio; e o limite por origem só vale de verdade quando houver, na borda, um proxy que escreva o `X-Forwarded-For`.

## Documentação

| | |
|---|---|
| [`docs/features/`](docs/features/) | cada área: regras, achados de segurança, o que os testes cobrem |
| [`docs/workflow/`](docs/workflow/) | GitFlow, CI/CD, code review, ambientes |
| [`docs/features/backup-e-deploy.md`](docs/features/backup-e-deploy.md) | como salvar, restaurar e conferir depois de publicar |
| [`CHANGELOG.md`](CHANGELOG.md) | o que mudou em cada versão |

Os documentos de feature existem para responder rápido a três perguntas: **isso é seguro?** (achados com identificador, separados em corrigidos, abertos e verificado-e-OK), **isso tem teste?** (cada teste ligado ao risco que protege, e o que **não** está coberto) e **como eu confiro em produção?** (comandos somente leitura, prontos para copiar).

## Contribuindo

Toda mudança entra por pull request contra `develop`, com os gates verdes: build, testes, cobertura, varredura de segredo e análise estática. `main` recebe apenas release e hotfix, sempre com tag.

```
main         produção, sempre com tag
develop      integração
feature/*    trabalho novo
release/*    versão em preparação
hotfix/*     correção urgente
```

Commits seguem [Conventional Commits](https://www.conventionalcommits.org/pt-br/). O processo está detalhado em [`docs/workflow/`](docs/workflow/).

## Estrutura

```
auth-service/            identidade, emissão de token e JWKS      Java · MySQL
product-service/         catálogo, imagens e dados da loja        Java · PostgreSQL
florescer-web/           vitrine, painel e proxy das APIs         Next.js
docs/features/           uma página por área, com os achados de segurança
docs/workflow/           GitFlow, CI/CD, code review, ambientes
scripts/                 backup, restauração e smoke test
.github/workflows/       CI, CD e análise estática
docker-compose.yml       a stack para desenvolver
docker-compose.prod.yml  a stack para publicar, com as APIs fechadas
```
