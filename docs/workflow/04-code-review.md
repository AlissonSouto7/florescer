# Code review: o que olhar num PR

Este documento é o guia de revisão do Florescer. Vale tanto para revisar código de outra pessoa quanto para revisar o próprio antes de abrir o PR.

## Para que serve a revisão

Não é para achar erro de digitação: linter faz isso. Revisão de código serve para três coisas que ferramenta nenhuma faz:

1. **Verificar se o código resolve o problema certo.** Um código impecável que resolve o problema errado é desperdício completo.
2. **Espalhar conhecimento.** Depois da revisão, duas pessoas entendem aquela parte do sistema em vez de uma.
3. **Achar o que os testes não pegam**: condição de corrida, buraco de autorização, suposição não declarada.

## A ordem de leitura

Revisar de cima para baixo, arquivo por arquivo, é a forma mais lenta e menos eficaz. A ordem que funciona:

1. **Leia a descrição do PR primeiro.** Se ela não explica o problema, devolva antes de olhar o código. Revisar sem contexto produz comentário de estilo, que é o comentário menos útil que existe.
2. **Olhe os testes antes da implementação.** Os testes descrevem o comportamento pretendido. Se o teste não deixa claro o que deveria acontecer, a implementação também não vai.
3. **Aí sim leia a implementação**, perguntando se ela faz o que os testes prometem.
4. **Procure o que não está no diff.** Essa é a parte difícil: o caso não tratado, o consumidor que ninguém atualizou, o rollback que não existe.

## O que perguntar, em ordem de importância

### Corretude

- O caso de borda existe? Entrada vazia, nula, negativa, gigante, duplicada.
- E se duas requisições dessas rodarem ao mesmo tempo? Toda operação que mexe em dinheiro, estoque, ou estado externo precisa dessa resposta com teste, não com opinião.
- O que acontece quando a dependência externa falha (timeout, 500, resposta malformada)?
- Se o código tem `catch` que desfaz algo, esse desfazer tem teste próprio? Código de recuperação sem teste é o que mais falha quando é acionado, porque nunca rodou.

### Segurança

Estas perguntas valem para todo PR que toque rota, controller, model, formulário, upload, query ou permissão:

- **Autorização por objeto, não só por rota.** Um middleware de admin não basta: `/algo/{id}` sem checar dono é IDOR.
- **Escalação de privilégio.** Quem pode promover cargo? Pode promover para nível igual ou acima do próprio? Pode editar a si mesmo?
- **Mass assignment.** Input cru caindo em `update()`/`fill()` deixa o cliente escrever campo sensível.
- **Validação sempre no servidor.** Regra no JavaScript é experiência de uso; o guard tem que existir no backend.
- **Segredo nunca em repo nem em log.** Vale para resposta de API e mensagem de exceção também.
- **Query parametrizada, saída escapada.**
- **Rate limit** em login, recuperação de senha e qualquer endpoint caro.

Achou um buraco fora do escopo do PR? **Reporte sempre**, mesmo sem corrigir na hora. Nunca deixe passar em silêncio por ser "de outra tarefa".

### Prova de que funciona

Este projeto exige quatro provas em todo PR (é o que o template pede):

|  | Antes | Depois |
|---|---|---|
| **Código** | teste automatizado que **falha**, com o output colado | o mesmo teste passando |
| **Humano** | o comportamento errado visto na tela, no log ou no banco | o comportamento certo, visto do mesmo jeito |

Só o "depois" não vale: sem o vermelho, ninguém sabe se o teste exercita o problema. Só o teste de código também não vale: teste verde com tela quebrada já aconteceu neste projeto.

**Teste que passa de primeira é suspeito.** Quebre o código de propósito e confirme que o teste acusa. Casos reais já pegos assim:

- `expect(...)->not->toContain($x, "mensagem")` — o framework tratou o segundo argumento como outro needle, e a negação passava sempre.
- Um mock sem `->once()` — passava mesmo quando o código nunca chamava o serviço.

### Dado é medido, nunca deduzido

Antes de escrever qualquer número, contagem, porcentagem ou afirmação de comportamento, a pergunta é: **eu medi isso ou eu deduzi?**

- Não afirme "isso não afeta X" sem rodar o grep ou o teste que prova.
- Não classifique severidade sem medir o alcance.
- Se não dá para medir, escreva "não verificado" e diga qual comando resolveria.

Número bonito sem evidência é pior que "não sei", porque induz alguém a subir para produção confiando em algo que ninguém checou.

### Manutenção

- Segue a convenção que já existe no projeto? Um PR não é o lugar de introduzir um estilo novo sem combinar antes.
- Dá para reusar algo que já existe em vez de criar? Este projeto já tem `SwaggerConstants`, `getProductOrThrow`, `ApiErrorResponse` — código duplicado costuma ser desconhecimento do que existe.
- O nome diz o que a coisa faz? O caso mais gritante aqui foi uma classe chamada `ProductRestController` que era, na verdade, a implementação do repositório.
- Comentário explica **restrição que o código não mostra**, não o que a linha faz. Comentário que narra a linha vira mentira no primeiro refactor.

## Como comentar

O comentário de revisão tem que dizer **o que**, **por quê** e **o que fazer**. Sem os três, vira ping-pong.

Ruim:
> Isso está errado.

Bom:
> `getOriginalFilename()` vem do cliente sem sanitização, então `filename="../../config/app.key"` escapa da pasta de uploads e `createDirectories` ainda cria o caminho de destino. Sugiro descartar o nome original e montar o nome só com UUID + extensão derivada dos magic bytes.

Separe o que bloqueia do que é preferência. Uma convenção simples e comum:

- **bloqueia**: precisa mudar antes do merge.
- **sugestão**: melhoraria, mas não impede.
- **dúvida**: não entendi, me explica.
- **nit**: preferência pessoal, ignore à vontade.

Marcar tudo como bloqueio faz o revisor perder credibilidade; não marcar nada faz a revisão virar carimbo.

## Do outro lado: recebendo revisão

- Comentário é sobre o código, não sobre você. A separação é aprendida, não natural.
- Se dois revisores entenderam errado a mesma coisa, o problema provavelmente é o código, não os revisores.
- Discordar é legítimo. Responda com o argumento, não com "ok" seguido de nada.
- PR pequeno recebe revisão melhor. Acima de ~400 linhas, a qualidade da revisão despenca: o revisor cansa e passa a aprovar no diagonal. Se o seu PR está grande, o conserto é quebrar a tarefa, não pedir mais atenção.

## Revisando sozinho

Num projeto de uma pessoa, o revisor é você mesmo, algumas horas depois. Duas coisas ajudam:

1. **Leia o diff no GitHub, não no editor.** O contexto visual diferente faz enxergar o que a familiaridade escondeu.
2. **Preencha o template do PR de verdade.** Ele foi feito para forçar as perguntas que você pularia. Se travar em "prova de antes e depois", achou uma lacuna real, não uma burocracia.
