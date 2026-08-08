# O que muda

<!-- Uma frase: o que este PR entrega. Se precisar de parágrafo, o PR provavelmente está grande demais. -->

Closes #

## Por quê

<!-- O problema que motivou a mudança, não a descrição do código. -->

## Prova de antes e depois

Sem as quatro provas o PR não é revisável. Cole os outputs, não descreva.

|  | Antes | Depois |
|---|---|---|
| **Código** (teste automatizado) | <!-- teste FALHANDO, output colado --> | <!-- o mesmo teste passando --> |
| **Humano** (visto na tela) | <!-- print, log do navegador ou valor no banco --> | <!-- o mesmo, agora correto --> |

- [ ] O teste foi provado **não-vacuoso**: quebrei o código de propósito e ele acusou.

## Checklist de segurança

Marque o que se aplica ao que este PR toca. Achou buraco fora do escopo? Reporte na descrição mesmo sem corrigir.

- [ ] Autorização por objeto, não só por rota (o recurso pertence a quem pediu?)
- [ ] Sem escalação de privilégio (quem pode promover/editar quem?)
- [ ] Entrada validada no servidor (tipo, faixa, tamanho, enum)
- [ ] Nenhum segredo em código, log, resposta de API ou commit
- [ ] Query parametrizada, saída escapada
- [ ] Concorrência considerada (duas requisições simultâneas quebram algo?)
- [ ] Não se aplica a este PR

## Avaliação por dimensão

Cada número vem de algo medido (teste que rodou, comando cujo output eu vi). Sem base, escreva "não verificado".

| Dimensão | % | Evidência |
|---|---|---|
| Funcional | | |
| Testado | | |
| Seguro | | |
| Seguro pra subir | | |
| Código limpo | | |

**O que NÃO está coberto:**

## Como revisar

<!-- Por onde começar a leitura e como rodar isso localmente. -->
