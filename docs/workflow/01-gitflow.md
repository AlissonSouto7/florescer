# GitFlow

O modelo de branches do Florescer, e por que este e não outro.

Um fluxo de branch responde três perguntas: onde o código novo nasce, onde ele fica enquanto amadurece, e o que exatamente está em produção agora. Sem isso, a branch de produção vira uma fila onde código pronto e código pela metade se misturam.

## O modelo

Duas branches de vida longa, que nunca são deletadas:

| Branch | O que ela representa | Quem pode escrever nela |
|---|---|---|
| `main` | Exatamente o que está (ou pode ir) em produção | Ninguém direto. Só merge de `release/*` ou `hotfix/*` |
| `develop` | O próximo release sendo montado | Ninguém direto. Só merge de `feature/*` |

E três tipos de branch de vida curta, criadas e deletadas o tempo todo:

| Branch | Sai de | Volta para | Para quê |
|---|---|---|---|
| `feature/<issue>-<slug>` | `develop` | `develop` | Uma tarefa. Ex: `feature/12-cors-config` |
| `release/x.y.z` | `develop` | `main` **e** `develop` | Congelar escopo, testar em staging, corrigir só o que for crítico |
| `hotfix/x.y.z` | `main` | `main` **e** `develop` | Corrigir produção sem esperar o próximo release |

### Por que a release branch volta para os dois lugares

Quando você corrige um bug na `release/1.2.0`, essa correção precisa ir pra `main` (senão produção não recebe) **e** pra `develop` (senão o próximo release nasce com o bug de volta). O mesmo vale para hotfix. Esquecer o merge de volta pra `develop` é a causa clássica de "esse bug já não tinha sido corrigido?".

### O ciclo completo, na prática

```
main     ────●────────────────────────────●──────────  tag v1.0.0, v1.1.0
              \                          /|
release        \                  ●─────● |            release/1.1.0 → staging
                \                /        \
develop  ──●─────●────●────●────●──────────●────────
            \        /    /
feature      ●──────●    /                             feature/12-cors-config
                        /
feature            ●───●                               feature/15-upload-hardening
```

## Como fica no dia a dia deste projeto

1. A tarefa existe como **issue** no board (ver [03-metodologias-ageis.md](03-metodologias-ageis.md)).
2. A branch nasce de `develop` com o número da issue no nome: `git switch develop && git pull && git switch -c feature/12-cors-config`.
3. Commits seguem [Conventional Commits](#conventional-commits).
4. Abre-se o **Pull Request** para `develop`. O CI roda automaticamente (ver [02-ci-cd-e-gates.md](02-ci-cd-e-gates.md)).
5. Nenhum merge acontece com CI vermelho: a branch protection bloqueia o botão.
6. Após o merge, a branch de feature é deletada. O histórico dela já está em `develop`.

## Conventional Commits

Formato: `tipo(escopo): descrição no imperativo`.

```
feat(auth): add rate limit to login endpoint
fix(product): reject upload with mismatched magic bytes
test(auth): cover duplicate email registration race
refactor(product): rename ProductRestController to ProductRepositoryImpl
docs(workflow): explain branch protection rules
chore(ci): bump testcontainers to 1.21.4
```

O tipo alimenta a geração de release notes e permite responder "o que mudou de comportamento entre v1.0 e v1.1?" filtrando por `feat` e `fix`. A descrição vai no imperativo ("add", não "added") porque a mensagem completa a frase "este commit vai...".

## Versionamento: SemVer

Uma tag `vX.Y.Z` na `main` marca cada release.

- **X (major)**: quebrou compatibilidade. Quem consome a API precisa mudar o código.
- **Y (minor)**: funcionalidade nova, compatível com o que existia.
- **Z (patch)**: correção de bug, sem funcionalidade nova.

A tag não é decoração: é ela que dispara o deploy de produção no pipeline, e é ela que permite fazer rollback para um ponto exato.

## Alternativas consideradas

GitFlow não é o padrão da indústria hoje. Foi criado em 2010, para software com releases versionados e instalados pelo cliente.

### GitHub Flow

Uma branch de vida longa só (`main`). Feature branch sai da `main`, PR, merge, deploy imediato.

Ganha quando: você faz deploy várias vezes por dia, tem só uma versão viva em produção (SaaS), e tem cobertura de testes boa o bastante pra confiar no merge.

Perde quando: você precisa manter versões antigas, ou o deploy tem janela/aprovação.

### Trunk-based development

Todo mundo commita em `main` (o "trunk") várias vezes por dia, em branches que vivem horas, não dias. Funcionalidade incompleta fica escondida atrás de **feature flag** em vez de ficar escondida numa branch.

Ganha quando: o time é maduro em testes automatizados e quer minimizar o custo de integração. É o que o livro *Accelerate* aponta como correlacionado a times de alta performance.

Perde quando: a suíte de testes não é confiável. Sem rede de proteção, trunk-based é jogar código não verificado em produção.

### Por que GitFlow aqui

O Florescer tem release versionado, um ambiente de staging antes de produção, e um gate de aprovação manual pra produção. GitFlow modela exatamente isso: a `release/*` é o que existe em staging, a tag na `main` é o que existe em produção.

Se o projeto virasse deploy contínuo com feature flags, GitHub Flow passaria a ser a escolha certa. O fluxo serve ao ritmo de release, não o contrário.

## A armadilha que este projeto tem hoje: histórico linear com release branch

**As duas proteções que a `main` e a `develop` usam entram em conflito com o modelo descrito acima**, e isso não é teoria: custou 55 arquivos em conflito na release 0.2.0.

### O que aconteceu, medido

A `main` exige `required_linear_history`, que proíbe commit de merge. Sob essa regra, o único jeito de a release chegar lá é **squash**: 49 commits viram um só, novo, cujo pai é a ponta antiga da `main`.

O efeito é que **os commits da `develop` nunca viram ancestrais da `main`**. As duas branches passam a ter o mesmo trabalho com sha diferente, e o git deixa de conseguir compará-las: cada arquivo que existe dos dois lados e não existia no ancestral comum vira conflito `add/add`.

Na 0.2.0, abrir o PR da release para a `main` deu isto:

```
CONFLICT (add/add): 55 arquivos
merge-base: 640f0aa  (um commit de infraestrutura, de antes de quase tudo)
```

Não era conflito de conteúdo. Era o git dizendo que não sabia que as duas branches eram a mesma coisa.

### Como foi resolvido, e por que foi seguro

Antes de resolver, foi medido o que a `main` tinha e a `develop` não:

```
arquivos só na main: 8   →   todos de florescer-frontend/
```

Só o frontend estático que o Next substituiu. Todo o resto a `develop` tinha em versão mais nova. Com isso, um merge da `main` para dentro da release pegando o lado da release (`-X ours`) não descarta nada que importe, e a prova foi comparar as árvores: **idênticas antes e depois**, ou seja, nada da `main` entrou.

Depois do back-merge, o mesmo teste de merge caiu de 55 conflitos para **6**, e os 6 são só os arquivos onde a `develop` está legitimamente à frente (as dependências que entraram depois da release ser cortada).

### O que fazer a respeito

O problema **volta a cada release**, porque a causa continua lá. Duas saídas, e as duas são escolha consciente:

| Saída | O que muda | Custo |
|---|---|---|
| **Desligar `required_linear_history` na `main` e na `develop`** | a release passa a chegar por commit de merge, e as histórias ficam ligadas para sempre | perde-se o histórico linear, que é uma propriedade boa de leitura |
| **Manter o squash e reconciliar a cada release** | nada muda no dia a dia | uma resolução manual por release, que cresce com o tamanho da versão |

Histórico linear combina com GitHub Flow e trunk-based, onde tudo entra por squash numa branch só. Com GitFlow, que existe justamente para ter duas linhas vivas, ele briga com o modelo. **Escolher os dois é escolher o conflito.**

Enquanto a decisão não é tomada, o procedimento da release é: cortar a branch da `develop`, mergear `origin/main` dentro dela resolvendo pelo lado da release, conferir que a árvore não mudou, e só então abrir o PR.

## Erros comuns (que este projeto já cometeu)

- **Commitar direto na `main` sem PR.** O histórico do Florescer começou assim: dois commits gigantes, um deles com mensagem "Substituindo conteúdo do repositório remoto", misturando três assuntos e uma chave privada. Impossível revisar, impossível reverter em partes.
- **Branch de vida longa que nunca integra.** Uma feature branch de duas semanas vira um merge doloroso. Se a tarefa é grande, quebre em issues menores que integrem sozinhas.
- **Esquecer o merge de volta na `develop`** depois de um hotfix.
- **Squashar a release na `main` sem trazer a `main` de volta.** O commit criado pelo squash não é ancestral da `develop`, e as duas branches se afastam em silêncio até a próxima release não conseguir mergear. Ver a seção sobre histórico linear acima.
- **Nome de branch sem o número da issue.** Meses depois, `feature/ajustes` não diz nada; `feature/12-cors-config` liga direto ao contexto da issue.
