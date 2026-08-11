<div align="center">

# Florescer

**Catálogo online para uma loja de plantas.**

[![CI](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/ci.yml)
[![CodeQL](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml/badge.svg?branch=develop)](https://github.com/AlissonSouto7/florescer/actions/workflows/codeql.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)](https://spring.io/projects/spring-boot)

</div>

---

O Florescer é uma vitrine pública onde cada planta tem foto, descrição, cuidados e preço, com uma API para manter esse catálogo atualizado.

## Estado atual

`v0.1.0` — o catálogo funciona de ponta a ponta e a API está pronta para produção.

| | |
|---|---|
| ✅ | Vitrine pública com foto, nome, descrição e preço |
| ✅ | Contas de usuário com registro e login |
| ✅ | API de catálogo completa, com upload de imagem |
| ✅ | Autorização por papel: leitura pública, escrita restrita a administrador |
| ⬜ | Botão de comprar levando ao WhatsApp da vendedora ([#61](https://github.com/AlissonSouto7/florescer/issues/61)) |
| ⬜ | Painel para a vendedora cadastrar planta sem usar a API ([#62](https://github.com/AlissonSouto7/florescer/issues/62)) |

Hoje o cadastro de plantas é feito pela API. Um painel para quem não é técnico está no roadmap.

## Arquitetura

Dois serviços independentes. O catálogo não consulta o serviço de identidade a cada requisição: ele valida a assinatura do token por conta própria, buscando a chave pública uma vez.

```
                          navegador
                              │
              ┌───────────────┴───────────────┐
              │                               │
     login / registro                      catálogo
              │                               │
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

Consequência prática: o catálogo continua no ar mesmo se o serviço de identidade cair, e trocar a chave de assinatura não exige redeploy dos dois lados.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5 |
| Segurança | Spring Security, OAuth2 Resource Server, JWT RS256 |
| Persistência | Spring Data JPA, Flyway |
| Bancos | MySQL 8.4, PostgreSQL 16 |
| Testes | JUnit 5, AssertJ, Testcontainers |
| Infra | Docker, GitHub Actions, GHCR |
| Frontend | HTML, CSS, JavaScript |

## Começando

**Requisitos:** JDK 17 e Docker. Maven não é necessário — o wrapper está versionado.

```bash
# 1. Gerar o par de chaves (fora do repositório)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out ~/florescer/app.key
openssl rsa -pubout -in ~/florescer/app.key -out ~/florescer/app.pub

# 2. Configurar
cp .env.example .env    # preencher conforme os comentários do arquivo

# 3. Subir
docker compose up --build
```

| Serviço | URL |
|---|---|
| Vitrine | http://localhost:3000 |
| API de identidade | http://localhost:8080/swagger |
| API de catálogo | http://localhost:8081/swagger |

Sem as chaves configuradas a aplicação não sobe. Isso é intencional: um valor padrão faria o sistema assinar tokens com uma chave conhecida por qualquer pessoa que leia o repositório.

### Problemas comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `jwt.private-key deve conter um PEM...` com o `.env` correto | variável de ambiente do sistema vence o `.env` no Compose | `echo $RSA_PRIVATE_KEY` e remova-a |
| `port is already allocated` | 3306, 5432, 8080, 8081 ou 3000 em uso | defina `AUTH_DB_PORT`, `PRODUCT_DB_PORT`, `AUTH_PORT`, `PRODUCT_PORT` ou `FRONTEND_PORT` no `.env` |
| chave PEM cortada no meio | o Compose lê só até a primeira quebra de linha | `awk 'BEGIN{ORS="\\n"}1' app.key` |

## Testes

```bash
cd auth-service && ./mvnw verify     # ou product-service
```

146 testes rodando contra MySQL e PostgreSQL reais via Testcontainers, com piso de cobertura obrigatório. Docker precisa estar ativo.

| | Testes | Linha | Ramo | Piso |
|---|---|---|---|---|
| auth-service | 62 | 89% | 70% | 80% / 55% |
| product-service | 84 | 84% | 65% | 75% / 55% |

Banco em memória não é usado: SQL específico, tipo `NUMERIC` e comportamento de transação diferem justamente onde os defeitos aparecem.

## API

<details>
<summary><b>auth-service</b> — identidade</summary>

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/v1/auth/register` | público |
| `POST` | `/v1/auth/login` | público |
| `GET` | `/.well-known/jwks.json` | público |
| `GET` | `/actuator/health` | público |

</details>

<details>
<summary><b>product-service</b> — catálogo</summary>

| Método | Rota | Acesso |
|---|---|---|
| `GET` | `/v1/product` | público |
| `GET` | `/v1/product/{id}` | público |
| `POST` | `/v1/product` | ADMIN |
| `PATCH` | `/v1/product/{id}` | ADMIN |
| `DELETE` | `/v1/product/{id}` | ADMIN |
| `GET` | `/uploads/**` | público |

</details>

## Segurança

Decisões que valem conhecer antes de mexer no código:

- Chaves RSA nunca no repositório; carregadas por configuração, sem valor padrão.
- Upload identifica o tipo pelos **bytes** do arquivo, não pelo `Content-Type` declarado, e descarta o nome original.
- Log registra pseudônimo, nunca e-mail.
- Respostas de erro não revelam se uma conta existe.
- Rate limit em login e registro, antes de chegar ao banco.
- Unicidade de e-mail garantida por constraint, não por verificação prévia.

Achados por área, incluindo os que continuam **abertos**, em [`docs/features/`](docs/features/).

## Documentação

| | |
|---|---|
| [`docs/features/`](docs/features/) | cada área: regras, achados de segurança, cobertura de teste |
| [`docs/workflow/`](docs/workflow/) | GitFlow, CI/CD, code review, ambientes |
| [`CHANGELOG.md`](CHANGELOG.md) | o que mudou em cada versão |

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
auth-service/         identidade e emissão de token
product-service/      catálogo e imagens
florescer-frontend/   páginas estáticas
docs/                 processo e documentação por feature
.github/workflows/    CI, CD e análise estática
docker-compose.yml    a stack completa
```
