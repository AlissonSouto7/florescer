# CI, CD e gates: o que impede código ruim de passar

Este documento explica o pipeline do Florescer: o que roda, quando roda, e o que exatamente bloqueia um merge ou um deploy.

## Os dois conceitos, sem confundir

**CI (Continuous Integration)** é sobre *integrar*: toda mudança é juntada com o código dos outros e verificada automaticamente, várias vezes ao dia. A pergunta que a CI responde é "isso quebra alguma coisa?".

**CD** tem dois significados diferentes que vivem sendo trocados:
- *Continuous Delivery*: toda mudança aprovada fica **pronta** para ir a produção, mas alguém decide quando. É o caso deste projeto.
- *Continuous Deployment*: toda mudança aprovada **vai** para produção automaticamente, sem humano no meio.

A diferença entre os dois é exatamente um botão de aprovação. O Florescer usa Delivery: produção exige aprovação manual.

## O que é um "gate"

Gate é qualquer regra que impede a mudança de avançar até uma condição ser satisfeita. Sem gate, o CI vira decoração: roda, fica vermelho, e alguém mergeia mesmo assim.

Os gates deste projeto, na ordem em que uma mudança os encontra:

| # | Gate | Onde | O que bloqueia |
|---|---|---|---|
| 1 | PR obrigatório | Branch protection | Push direto em `main`/`develop` |
| 2 | Build + testes verdes | Workflow `ci.yml`, job `build` | Merge do PR |
| 3 | Coverage gate | JaCoCo, dentro do mesmo job | Merge do PR |
| 4 | Secret scan | Workflow `ci.yml`, job `secret-scan` | Merge do PR |
| 5 | Revisão humana | CODEOWNERS + leitura do PR | Merge do PR |
| 6 | Aprovação de environment | GitHub Environments | Deploy em produção |

### Gate 1: pull request obrigatório

Configurado em Settings → Branches → Branch protection rules. Com ele ligado, `git push origin main` é recusado pelo servidor. Toda mudança entra por PR, o que garante que exista um lugar para revisar antes de integrar.

### Gate 2: status check obrigatório

Um "status check" é o resultado de um job do workflow reportado de volta ao PR. Quando o job `build (auth-service)` é marcado como **required**, o botão de merge fica desabilitado até ele ficar verde.

O job roda `./mvnw verify`, que faz, nesta ordem: compila, roda os testes, gera o relatório de cobertura, aplica o gate de cobertura. Se qualquer etapa falhar, o job falha, e o merge trava.

Detalhe importante do `ci.yml`: a matriz roda os dois serviços em paralelo com `fail-fast: false`. Se o auth quebrar, ainda quero saber se o product passa — resultado parcial economiza uma rodada inteira de correção.

### Gate 3: coverage gate

O JaCoCo mede quantas linhas do código foram executadas pelos testes e falha o build se ficar abaixo do mínimo. No `pom.xml` de cada serviço:

```xml
<jacoco.line.coverage.minimum>0.00</jacoco.line.coverage.minimum>
```

Está em zero **de propósito** neste momento: o projeto tem praticamente nenhum teste, e um gate que reprova tudo desde o primeiro dia seria desligado na primeira semana. O número sobe a cada fase de testes entregue, e o valor sempre reflete a cobertura real medida, nunca uma meta aspiracional.

Uma armadilha que vale saber explicar: **cobertura alta não significa código testado**. Um teste que executa a linha mas não verifica nada conta como cobertura. Por isso o gate de cobertura é o mais fraco dos gates; o que realmente protege é o protocolo de teste vermelho antes do verde (ver [04-code-review.md](04-code-review.md)).

### Gate 4: secret scan

O Gitleaks varre os commits do PR procurando padrões de segredo (chave privada, token, credencial). Existe porque este repositório já teve uma chave privada RSA commitada, que assinava todos os JWTs do sistema, e ninguém percebeu por dois meses. O `.gitignore` agora bloqueia `*.key` e `*.pem`, mas gitignore só protege quem não usa `git add -f`.

### Gate 5: revisão humana

O arquivo `.github/CODEOWNERS` marca automaticamente o dono do repositório como revisor de todo PR.

Em repositório de uma pessoa só há um limite real: o GitHub não permite aprovar formalmente o próprio PR. Então "required approvals" fica em zero e a revisão acontece na leitura antes de clicar em merge. Num time, esse é o gate mais importante de todos, e o número seria 1 ou 2.

### Gate 6: environment protection

Este é o gate de deploy, e é o mais fácil de explicar em entrevista porque é visual: o job fica **pausado**, com um botão "Review deployments", até um humano aprovar.

Configurado em Settings → Environments. O Florescer tem três:

| Environment | Alimentado por | Aprovação |
|---|---|---|
| `development` | push em `develop` | automático |
| `staging` | push em `release/*` | automático |
| `production` | tag `v*.*.*` | **exige aprovação manual** |

No `cd.yml`, o vínculo é a linha `environment: ${{ needs.resolve.outputs.environment }}` no job. É isso, e só isso, que faz o GitHub aplicar as regras de proteção configuradas.

## Promoção de artefato: o conceito que mais cai em entrevista

A regra é: **a mesma imagem que passou em staging é a que vai para produção**. Nunca se reconstrói para promover.

Se você recompila a cada ambiente, o binário testado em staging não é o binário que roda em produção. Uma dependência pode ter mudado, o cache pode ter sido invalidado, a base image pode ter recebido patch. Aí "funcionou em staging" não prova nada.

No `cd.yml` isso aparece na tag dupla:

```yaml
tags: |
  ...auth-service:${{ needs.resolve.outputs.tag }}
  ...auth-service:${{ github.sha }}
```

A tag de commit (`sha`) identifica o binário exato de forma imutável. A tag de ambiente (`dev`, `1.2.0-rc`, `1.2.0`) é um apelido que aponta para ele.

## Anatomia de um workflow do GitHub Actions

Vocabulário mínimo, na ordem de aninhamento:

- **Workflow**: o arquivo `.yml` inteiro. Um por objetivo (`ci.yml`, `cd.yml`).
- **Trigger** (`on:`): o que dispara. Aqui: `pull_request`, `push` em branches, `push` de tags.
- **Job**: uma unidade que roda numa máquina própria (`runs-on: ubuntu-latest`). Jobs rodam em paralelo por padrão; `needs:` cria dependência.
- **Step**: um comando ou uma action dentro do job. Compartilham o mesmo disco.
- **Action**: um passo reutilizável de terceiros (`actions/checkout@v4`).
- **Matrix**: roda o mesmo job N vezes com valores diferentes. Aqui, um por serviço.
- **Secret**: valor cifrado guardado no GitHub, injetado como variável de ambiente. Nunca aparece no log.
- **Concurrency**: cancela execução anterior da mesma branch quando chega push novo.
- **Permissions**: o que o token do workflow pode fazer. O `ci.yml` usa `contents: read` (o mínimo); o `cd.yml` precisa de `packages: write` para publicar no registry.

## Por que os testes usam banco de verdade no CI

Os testes sobem MySQL e PostgreSQL reais em container (Testcontainers), não banco em memória.

Banco em memória tem SQL, tipos, collation e comportamento transacional diferentes do banco de produção. Um teste que passa em H2 e quebra em MySQL não é um teste, é uma falsa sensação de segurança. O runner do GitHub Actions tem Docker disponível, então o mesmo teste roda igual na máquina do dev e no CI.

Custa mais tempo (o container leva alguns segundos para subir) e vale cada segundo.

## Como ler um pipeline vermelho

1. Abra a aba **Actions** ou clique em "Details" no check do PR.
2. Ache o job vermelho e o **primeiro** step vermelho. Os erros seguintes costumam ser consequência.
3. Se for teste, baixe o artefato `test-reports-<serviço>` — o `ci.yml` sobe os relatórios do surefire e da cobertura com `if: always()`, ou seja, **mesmo quando o build falha**. É a diferença entre debugar com o erro em mãos e adivinhar.
4. Reproduza localmente com o mesmo comando do workflow: `./mvnw verify`. Se passa local e falha no CI, a diferença está no ambiente (variável de ambiente ausente, arquivo não commitado, dependência de estado da sua máquina).

O caso real mais recente aqui: o Testcontainers 1.21.0 não fala a API do Docker Engine 29, e o erro que aparecia era um genérico "Could not find a valid Docker environment". A causa só ficou visível ao ler o log completo, onde o daemon respondia HTTP 400. Lição: erro genérico raramente tem causa genérica.
