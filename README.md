# Florescer

[![CI](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml)
[![CodeQL](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml)

Loja de plantas. Um catálogo público que qualquer pessoa navega sem conta, e uma área administrativa onde quem tem permissão cadastra, edita e remove produtos com foto.

O sistema é dividido em dois serviços com responsabilidades separadas: um cuida de identidade, o outro de catálogo. O de catálogo não pergunta ao de identidade quem é a pessoa a cada requisição; ele verifica a assinatura do token e decide sozinho.

## Como as peças se encaixam

```
                    navegador
                        │
        ┌───────────────┼───────────────┐
        │ login/registro│               │ catálogo
        ▼               │               ▼
┌────────────────┐      │      ┌──────────────────┐
│  auth-service  │      │      │ product-service  │
│     :8080      │      │      │      :8081       │
│                │      │      │                  │
│ assina o token │      │      │ valida o token   │
│ com a chave    │      │      │ com a chave      │
│ PRIVADA        │      │      │ PÚBLICA          │
└───────┬────────┘      │      └────────┬─────────┘
        │               │               │
        │   /.well-known/jwks.json      │
        │◄──────────────────────────────┘
        │      (busca a chave pública)
        ▼                               ▼
   ┌─────────┐                    ┌──────────┐
   │  MySQL  │                    │ Postgres │
   │  :3306  │                    │  :5432   │
   └─────────┘                    └──────────┘
```

Os serviços **não** conversam por HTTP para autenticar. O auth-service assina um JWT com RS256 e publica a chave pública em `/.well-known/jwks.json`; o product-service busca essa chave e valida a assinatura por conta própria. Isso significa que o catálogo continua respondendo mesmo se o serviço de identidade estiver fora do ar, e que trocar a chave não exige coordenar um redeploy dos dois.

O papel viaja dentro do token, na claim `scope`, e vira uma autoridade `ROLE_` do lado que valida.

## Stack

| | |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5 |
| Segurança | Spring Security, OAuth2 Resource Server, JWT RS256 |
| Persistência | Spring Data JPA, Flyway |
| Bancos | MySQL 8.4 (auth), PostgreSQL 16 (product) |
| Testes | JUnit 5, AssertJ, Testcontainers |
| Cobertura | JaCoCo, com piso obrigatório no CI |
| Frontend | HTML, CSS e JavaScript sem framework |

## Portas

| Serviço | Porta | Observação |
|---|---|---|
| auth-service | 8080 | |
| product-service | 8081 | |
| frontend | 3000 | apenas via Docker |
| MySQL | 3306 | publicado só em `127.0.0.1` |
| PostgreSQL | 5432 | publicado só em `127.0.0.1` |

## Rodando

### Pré-requisitos

- JDK 17
- Docker (obrigatório: os testes sobem bancos reais com Testcontainers)

O Maven não precisa estar instalado. O wrapper (`./mvnw`) está versionado, então a versão usada aqui é a mesma da sua máquina e a mesma do CI.

### 1. Gere o par de chaves RSA

**Um clone limpo não sobe sem isto.** As chaves não têm valor padrão de propósito: um default faria a aplicação subir assinando tokens com uma chave que ninguém escolheu, e que estaria no repositório à vista de todos. Falhar no boot é o comportamento correto.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out app.key
openssl rsa -pubout -in app.key -out app.pub
```

Guarde os arquivos **fora do repositório**. O `.gitignore` já bloqueia `*.key` e `*.pem`, mas a proteção real é não colocar a chave aqui dentro.

### 2. Configure o ambiente

```bash
cp .env.example .env
```

Preencha o `.env`. O arquivo explica cada variável; o mínimo é `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` e o caminho das duas chaves.

Duas armadilhas que já custaram tempo aqui:

**Variável de ambiente vence o `.env`.** Se `RSA_PRIVATE_KEY` existir no ambiente do seu sistema, o Compose usa aquela e ignora a do arquivo. O sintoma é a aplicação morrer no boot dizendo que a chave não é um PEM válido, o que faz procurar defeito no `.env`, onde não há nenhum. Confira com `echo $RSA_PRIVATE_KEY` antes de investigar outra coisa.

**A chave em PEM precisa caber numa linha.** O Compose para de ler o valor na primeira quebra de linha. Troque as quebras por `\n` literal:

```bash
awk 'BEGIN{ORS="\\n"}1' app.key
```

**Portas em uso.** Se 3306, 5432, 8080, 8081 ou 3000 já estiverem ocupadas por outro projeto, defina `AUTH_DB_PORT`, `PRODUCT_DB_PORT`, `AUTH_PORT`, `PRODUCT_PORT` ou `FRONTEND_PORT` no `.env`. Só o lado do host muda; dentro da rede do Compose nada é afetado.

### 3. Suba

Tudo de uma vez, com os bancos, os dois serviços e o frontend:

```bash
docker compose up --build
```

O frontend fica em <http://localhost:3000>. Os serviços esperam os bancos ficarem prontos de verdade (`service_healthy`), não apenas o container subir.

Para rodar um serviço direto na máquina, com os bancos no Docker:

```bash
docker compose up auth-db product-db -d
cd auth-service && ./mvnw spring-boot:run
```

## Testes

```bash
cd auth-service && ./mvnw verify      # ou product-service
```

`verify` roda os testes **e** o piso de cobertura. Docker precisa estar rodando: os testes sobem MySQL e PostgreSQL reais em container, o mesmo motor da produção. Banco em memória não serve aqui, porque SQL específico, tipo `NUMERIC` e comportamento de transação diferem justamente onde os bugs moram.

A primeira execução baixa as imagens e demora mais.

## Documentação

| | |
|---|---|
| [`docs/workflow/`](docs/workflow/) | como trabalhamos: GitFlow, CI/CD e gates, Kanban, code review, ambientes |
| [`docs/features/`](docs/features/) | cada área do sistema, com regras, achados de segurança e o que os testes cobrem |
| Swagger do auth | <http://localhost:8080/swagger> |
| Swagger do product | <http://localhost:8081/swagger> |

## Saúde dos serviços

```
GET http://localhost:8080/actuator/health/readiness
GET http://localhost:8081/actuator/health/readiness
```

Só `health` está exposto. O resto do Actuator descreve a configuração interna da aplicação e fica fora.

## Como trabalhamos

Toda mudança entra por pull request contra `develop`, com os gates do CI verdes: build, testes, piso de cobertura, varredura de segredo e análise estática. `main` recebe apenas release e hotfix, sempre com tag.

Os guias em [`docs/workflow/`](docs/workflow/) explicam cada parte, incluindo o porquê de cada decisão.

## Estrutura

```
auth-service/       identidade: registro, login, emissão de token, JWKS
product-service/    catálogo: CRUD de produto e upload de imagem
florescer-frontend/ páginas estáticas
docs/               guias de processo e documentação por feature
.github/workflows/  CI, CD e análise estática
docker-compose.yml  a stack inteira
```
