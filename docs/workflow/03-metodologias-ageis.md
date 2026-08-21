# Como o trabalho é organizado

Kanban com board no GitHub Projects. Sem sprint.

## Por que Kanban e não Scrum

Scrum organiza o trabalho em ciclos de duração fixa (a sprint), com escopo combinado no início e revisado no fim. Funciona quando há um time para sincronizar e um ritmo de entrega previsível a sustentar.

Kanban não tem caixa de tempo. O que se limita é a quantidade de trabalho simultâneo (o WIP), e a tarefa flui quando fica pronta.

Aqui o trabalho chega por descoberta, não por planejamento de duas semanas: uma varredura de segurança abre cinco issues que não existiam ontem. Um ciclo de escopo fixo seria replanejado toda semana, o que é o mesmo que não ter ciclo.

A diferença em uma linha: Scrum fixa o tempo e ajusta o escopo; Kanban não fixa tempo e limita o trabalho em andamento.

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

## Métricas

O que este projeto acompanha:

- **Issues fechadas por entrega**, para saber o tamanho real de uma release.
- **Tempo entre abrir e fechar uma issue**, que revela tarefa grande demais.
- **Cobertura medida**, com piso obrigatório no CI.

O que não acompanha, e por quê:

- **Linhas de código**: mais código costuma ser pior, não melhor.
- **Story points**: sem time, estimar não informa nada que a issue já não diga.
- **Cobertura como meta**: 100% com asserções fracas é pior que 60% com testes que verificam de verdade, porque cria confiança falsa.
