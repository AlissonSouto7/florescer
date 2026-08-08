# Kanban, Scrum e como empresas realmente organizam o trabalho

Este documento explica o método de trabalho do Florescer e o vocabulário ágil que aparece em entrevista. A parte mais útil não é decorar a cerimônia, é entender o problema que cada uma resolve.

## O ponto de partida: o manifesto ágil

Em 2001, dezessete pessoas escreveram quatro valores em reação ao modelo em cascata (levantar todos os requisitos → projetar tudo → construir tudo → testar tudo → entregar, com meses entre as etapas). O problema do cascata é que o requisito envelhece antes da entrega, e o erro só aparece no fim, quando corrigir é caro.

Os quatro valores, na formulação original: *indivíduos e interações* acima de processos e ferramentas; *software funcionando* acima de documentação abrangente; *colaboração com o cliente* acima de negociação de contrato; *responder a mudanças* acima de seguir um plano.

O detalhe que quase todo mundo esquece ao citar: a frase termina com "*ou seja, mesmo havendo valor nos itens à direita, valorizamos mais os itens à esquerda*". Documentação não é inimiga. Plano não é inimigo. A prioridade é que mudou.

Ágil não é ausência de processo. É processo com ciclo curto de feedback.

## Scrum

Scrum organiza o trabalho em **sprints**: períodos fixos (normalmente duas semanas) ao fim dos quais existe algo entregável.

**Papéis:**
- *Product Owner*: decide a prioridade, representa o negócio, é dono do backlog.
- *Scrum Master*: cuida do processo e remove impedimentos. Não é chefe do time.
- *Time de desenvolvimento*: quem constrói. Se auto-organiza.

**Cerimônias:**
- *Sprint Planning*: o time escolhe o que cabe na sprint.
- *Daily* (15 min): o que fiz, o que farei, o que me trava. O valor real está no terceiro item; os dois primeiros o board já mostra.
- *Sprint Review*: demonstração do que ficou pronto, para quem pediu.
- *Retrospectiva*: o que melhorar no **processo**. É a cerimônia mais valiosa e a primeira que os times abandonam quando apertam o prazo.
- *Refinement*: quebrar e estimar itens do backlog antes de entrarem numa sprint.

**Estimativa por story points**: em vez de estimar horas (que todo mundo erra), estima-se complexidade relativa, geralmente em Fibonacci (1, 2, 3, 5, 8, 13). O time descobre com o tempo quantos pontos consegue entregar por sprint (a *velocity*) e passa a usar isso para planejar. O ponto não é acertar a estimativa, é ter uma unidade estável de comparação.

Scrum ganha quando o trabalho dá para ser planejado em blocos e existe alguém do negócio pedindo entregas previsíveis.

## Kanban

Kanban vem do sistema de produção da Toyota. Não tem sprint, não tem papel obrigatório, não tem estimativa obrigatória. Tem quatro práticas:

1. **Visualizar o fluxo.** O board mostra cada trabalho e em que etapa ele está.
2. **Limitar o trabalho em progresso (WIP limit).** Cada coluna tem um teto de cartões.
3. **Gerenciar o fluxo.** A métrica principal é *lead time*: quanto tempo um item leva da entrada até o fim.
4. **Melhorar continuamente** com base no que o fluxo mostra.

**O WIP limit é o coração do Kanban e o mais mal compreendido.** A intuição diz que fazer cinco coisas ao mesmo tempo entrega mais rápido. Acontece o contrário: cinco itens em progresso significa cinco itens *incompletos*, cada troca de contexto custa tempo, e nada chega ao fim. Limitar o WIP força terminar antes de começar. Quando a coluna está cheia, a resposta certa não é "abro mais uma", é "vou ajudar a destravar o que está lá".

Kanban ganha quando o trabalho chega de forma imprevisível (suporte, bugs, manutenção) ou quando o time é pequeno demais para sustentar as cerimônias do Scrum.

Na prática, muitas empresas usam **Scrumban**: board e WIP limit do Kanban, com daily e retrospectiva do Scrum, sem estimativa em pontos.

## Como o Florescer trabalha

Kanban, com board em GitHub Projects. Time de uma pessoa não sustenta cerimônia de Scrum, e o trabalho aqui chega por descoberta (uma análise de segurança gera N tarefas de tamanhos muito diferentes), o que combina melhor com fluxo contínuo do que com sprint fechada.

### As colunas

| Coluna | O que significa | WIP |
|---|---|---|
| **Backlog** | Identificado, ainda não detalhado | sem limite |
| **Ready** | Tem critério de aceite claro, dá para começar hoje | sem limite |
| **In Progress** | Alguém está trabalhando agora | **1** |
| **In Review** | PR aberto, esperando revisão e CI | 3 |
| **Done** | Mergeado em `develop` | — |

O WIP de 1 em "In Progress" é o que impede começar cinco tarefas e terminar nenhuma. Enquanto um PR espera revisão, ele está em "In Review" e não bloqueia o início do próximo item.

### O que é "Ready" de verdade

Uma issue só sai de Backlog quando responde três perguntas: **o que precisa existir**, **por quê** e **como saber que terminou** (critério de aceite verificável). É o que os templates em `.github/ISSUE_TEMPLATE/` obrigam a preencher.

Issue vaga é a causa mais comum de retrabalho: "melhorar a segurança do upload" não tem fim definido; "rejeitar upload cujos magic bytes não batem com o content-type declarado, com teste provando que um `.html` renomeado para `.jpg` é recusado" tem.

### Definition of Done

Este projeto considera pronto quando:

- [ ] O critério de aceite da issue está atendido
- [ ] Existe teste automatizado que falhava antes e passa depois
- [ ] O teste foi provado não-vacuoso (quebrei o código e ele acusou)
- [ ] O comportamento foi verificado à mão também (tela, log ou banco)
- [ ] O CI está verde, incluindo o gate de cobertura
- [ ] A documentação da feature foi atualizada junto, não depois
- [ ] O PR foi revisado e mergeado

"Definition of Done" existe para impedir a discussão de "mas está pronto?" acontecer no fim de cada tarefa. É acordo prévio.

### Rastreabilidade: issue → branch → PR → commit

Cada item tem um número que atravessa tudo:

```
issue #12 "Configurar CORS nos dois serviços"
  └── branch feature/12-cors-config
       └── PR "feat(auth,product): allow configurable CORS origins" (Closes #12)
            └── commits
```

Escrever `Closes #12` no corpo do PR faz o GitHub fechar a issue automaticamente no merge. Isso não é conveniência: é o que permite, meses depois, partir de uma linha de código e chegar na discussão que a originou.

## Métricas que importam (e as que enganam)

Úteis:
- **Lead time**: da criação da issue até o merge. Mede o sistema inteiro.
- **Cycle time**: do início do trabalho até o merge. Mede a execução.
- **Throughput**: itens concluídos por semana.

As quatro métricas DORA, que a pesquisa do livro *Accelerate* correlacionou com performance de entrega: frequência de deploy, lead time para mudança, taxa de falha de mudança e tempo de restauração de serviço.

Enganosas:
- **Linhas de código**: mais código costuma ser pior, não melhor.
- **Story points por pessoa**: vira competição e inflação de estimativa.
- **Cobertura de teste como meta**: 100% de cobertura com asserções fracas é pior que 60% com testes que realmente verificam, porque cria confiança falsa.

## O que responder numa entrevista

Se perguntarem "você já trabalhou com metodologia ágil?", a resposta fraca é listar cerimônias. A resposta forte descreve o fluxo concreto e o porquê das escolhas:

> "Uso Kanban com board no GitHub Projects. Cada tarefa vira issue com critério de aceite, a branch carrega o número da issue, e o PR fecha ela automaticamente. Mantenho WIP de 1 em progresso porque trabalho sozinho e o custo de troca de contexto é alto. Não uso sprint porque o trabalho chega por descoberta, não por planejamento de duas semanas. A Definition of Done exige teste vermelho antes do verde e CI verde, então 'pronto' não é opinião."

E se perguntarem a diferença entre Scrum e Kanban: Scrum é *timeboxed* (o tempo é fixo, o escopo se ajusta), Kanban é *flow-based* (não tem caixa de tempo, o que se limita é a quantidade de trabalho simultâneo).
