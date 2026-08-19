# Processo de desenvolvimento

Como o código anda, o que impede código ruim de passar, e como o trabalho é organizado. Cada documento registra a decisão tomada e a alternativa descartada.

| Documento | Responde |
|---|---|
| [01 - GitFlow](01-gitflow.md) | Onde o código nasce, como ele chega em produção, e por que não usamos GitHub Flow |
| [02 - CI, CD e gates](02-ci-cd-e-gates.md) | O que roda no pipeline e o que exatamente bloqueia um merge ou um deploy |
| [03 - Organização do trabalho](03-metodologias-ageis.md) | Kanban, WIP limit, Definition of Done, rastreabilidade |
| [04 - Code review](04-code-review.md) | O que olhar num PR, em que ordem, e como comentar |
| [05 - Ambientes e deploy](05-ambientes-e-deploy.md) | Por que três ambientes, promoção de artefato, rollback |

## O ciclo

```
issue no board (Ready)
  └─ branch feature/<issue>-<slug> a partir de develop
       └─ commits em Conventional Commits
            └─ PR para develop, template preenchido com as 4 provas
                 └─ CI: build + testes + cobertura + secret scan
                      └─ revisão humana
                           └─ merge (a issue fecha sozinha via "Closes #N")
                                └─ CD publica a imagem :dev
```

Depois, quando o escopo de um release fecha: `release/x.y.z` → staging → merge na `main` com tag `vx.y.z` → produção, atrás de aprovação manual.

## As regras que não se negociam

1. **Nada entra em `main` ou `develop` sem PR.** Push direto é bloqueado pelo servidor.
2. **Teste vermelho antes do verde.** Sem o vermelho, ninguém sabe se o teste exercita o problema.
3. **Teste que passa de primeira é suspeito.** Quebre o código de propósito e confirme que ele acusa.
4. **Todo dado é medido, nunca deduzido.** Se não mediu, escreva "não verificado".
5. **Testes rodam contra o banco de produção** (MySQL e PostgreSQL reais em container), nunca banco em memória.
6. **Achou buraco de segurança fora do escopo? Reporte mesmo sem corrigir.**
