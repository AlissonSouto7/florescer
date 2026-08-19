# CI, CD e gates

O pipeline do Florescer: o que roda, quando roda, e o que bloqueia um merge ou um deploy.

## CI e CD neste projeto

**CI (Continuous Integration)**: toda mudança é integrada ao código dos outros e verificada automaticamente. Responde "isso quebra alguma coisa?".

**CD** tem dois significados, frequentemente trocados:
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
| 4 | Secret scan | Push protection nativo + `ci.yml`, job `secret-scan` | O push, e depois o merge |
| 5 | Revisão humana | CODEOWNERS + leitura do PR | Merge do PR |
| 6 | Aprovação de environment | GitHub Environments | Deploy em produção |

Além dos gates, três verificações rodam **fora** do caminho crítico do merge, porque medem coisas que não mudam com o seu commit:

| Ferramenta | O que enxerga | Quando roda |
|---|---|---|
| **Dependabot** | dependências declaradas: Maven dos dois serviços, actions do pipeline, imagens base | continuamente, abrindo PR com a correção |
| **CodeQL** | o código que nós escrevemos: injeção, path traversal, desserialização insegura | em PR e semanalmente |
| **Trivy** | a imagem construída: distribuição base, JRE, bibliotecas de sistema | no CD, a cada imagem publicada |

A divisão não é arbitrária, é por **o que cada um consegue ver**. Dependabot lê arquivos de manifesto e não sabe nada sobre o sistema operacional dentro da imagem. Trivy inspeciona a imagem pronta e não entende a lógica do seu código. CodeQL segue o fluxo do dado no seu código e não sabe qual versão de biblioteca você declarou. Três pontos cegos diferentes, três ferramentas.

Este projeto começou com o OWASP dependency-check rodando em cada PR e foi trocado depois da primeira execução real: ele baixava 374.301 registros da base de vulnerabilidades, levou mais de 13 minutos e ainda estava em 43% quando a execução foi cancelada. Estava reprocessando, a cada pull request, uma base que muda com o tempo e não com o commit. Vulnerabilidade nova aparece porque alguém no mundo publicou uma CVE, não porque você mexeu no código, e é por isso que esse tipo de verificação pertence a um agendamento ou a um serviço contínuo, não ao caminho do merge.

### Gate 1: pull request obrigatório

Configurado em Settings → Branches → Branch protection rules. Com ele ligado, `git push origin main` é recusado pelo servidor. Toda mudança entra por PR, o que garante que exista um lugar para revisar antes de integrar.

### Gate 2: status check obrigatório

Um "status check" é o resultado de um job do workflow reportado de volta ao PR. Quando o job `build (auth-service)` é marcado como **required**, o botão de merge fica desabilitado até ele ficar verde.

O job roda `./mvnw verify`, que faz, nesta ordem: compila, roda os testes, gera o relatório de cobertura, aplica o gate de cobertura. Se qualquer etapa falhar, o job falha, e o merge trava.

Detalhe importante do `ci.yml`: a matriz roda os dois serviços em paralelo com `fail-fast: false`. Se o auth quebrar, o resultado do product ainda interessa: saber os dois de uma vez economiza uma rodada de correção.

### Gate 3: coverage gate

O JaCoCo mede quantas linhas do código foram executadas pelos testes e falha o build se ficar abaixo do mínimo. No `pom.xml` de cada serviço:

```xml
<jacoco.line.coverage.minimum>0.00</jacoco.line.coverage.minimum>
```

Está em zero **de propósito** neste momento: o projeto tem praticamente nenhum teste, e um gate que reprova tudo desde o primeiro dia seria desligado na primeira semana. O número sobe a cada fase de testes entregue, e o valor sempre reflete a cobertura real medida, nunca uma meta aspiracional.

Uma armadilha que vale saber explicar: **cobertura alta não significa código testado**. Um teste que executa a linha mas não verifica nada conta como cobertura. Por isso o gate de cobertura é o mais fraco dos gates; o que realmente protege é o protocolo de teste vermelho antes do verde (ver [04-code-review.md](04-code-review.md)).

### Gate 4: secret scan

O Gitleaks varre os commits do PR procurando padrões de segredo (chave privada, token, credencial). Existe porque este repositório já teve uma chave privada RSA commitada, que assinava todos os JWTs do sistema, num repositório **público**, e ninguém percebeu por dois meses. O `.gitignore` agora bloqueia `*.key` e `*.pem`, mas gitignore só protege quem não usa `git add -f`.

Acima dele existe uma camada melhor: o **push protection** nativo do GitHub, ativado nas configurações do repositório. Ele recusa o push no momento em que a credencial sairia da sua máquina. A diferença importa: o Gitleaks no CI avisa quando o segredo **já está** no servidor, e a partir daí o estrago está feito, porque remover do histórico não desfaz quem já leu. O scan no CI vira a segunda linha, para o que o push protection não reconhece.

#### Falso positivo: silenciar o alarme ou ajustar a regra

Na primeira execução real deste pipeline, o Gitleaks reprovou o PR apontando `JwtConfig.java`:

```java
private static final String PEM_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
```

Não é uma chave, é o rótulo do formato, usado para validar que o valor recebido é mesmo um PEM. Mas para uma ferramenta que procura padrões de texto, esse é exatamente o padrão procurado.

A saída rápida seria mandar ignorar o arquivo inteiro. Isso resolveria o vermelho e deixaria justamente o arquivo que manipula chaves fora do escaneamento: o alarme silenciado exatamente onde ele mais importa. O que foi feito, no `.gitleaks.toml`, foi uma exceção estreita, que só aceita o texto quando ele está sendo atribuído a uma constante `PEM_*_HEADER` ou passado a um `replace()`.

E a exceção foi verificada como se verifica um teste: plantando uma chave privada de verdade num arquivo Java e rodando o scan de novo. Continuou detectando. Toda vez que você afrouxa uma regra de segurança, o passo seguinte é provar que ela ainda pega o caso real.

### Gate 5: revisão humana

O arquivo `.github/CODEOWNERS` marca automaticamente o dono do repositório como revisor de todo PR.

Em repositório de uma pessoa só há um limite real: o GitHub não permite aprovar formalmente o próprio PR. Então "required approvals" fica em zero e a revisão acontece na leitura antes de clicar em merge. Num time, esse é o gate mais importante de todos, e o número seria 1 ou 2.

### Gate 6: environment protection

O gate de deploy. O job fica **pausado**, com um botão "Review deployments", até alguém aprovar.

Configurado em Settings → Environments. O Florescer tem três:

| Environment | Alimentado por | Aprovação |
|---|---|---|
| `development` | push em `develop` | automático |
| `staging` | push em `release/*` | automático |
| `production` | tag `v*.*.*` | **exige aprovação manual** |

No `cd.yml`, o vínculo é a linha `environment: ${{ needs.resolve.outputs.environment }}` no job. É isso, e só isso, que faz o GitHub aplicar as regras de proteção configuradas.

## Promoção de artefato

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
3. Se for teste, baixe o artefato `test-reports-<serviço>`. O `ci.yml` sobe os relatórios do surefire e da cobertura com `if: always()`, ou seja, **mesmo quando o build falha**.
4. Reproduza localmente com o mesmo comando do workflow: `./mvnw verify`. Se passa local e falha no CI, a diferença está no ambiente (variável de ambiente ausente, arquivo não commitado, dependência de estado da sua máquina).

O caso real mais recente aqui: o Testcontainers 1.21.0 não fala a API do Docker Engine 29, e o erro que aparecia era um genérico "Could not find a valid Docker environment". A causa só ficou visível ao ler o log completo, onde o daemon respondia HTTP 400. Lição: erro genérico raramente tem causa genérica.
