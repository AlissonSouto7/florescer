# Ambientes, deploy e rollback

Este documento explica por que existem três ambientes no Florescer, o que diferencia cada um, e como uma mudança viaja do commit até a produção.

## Por que mais de um ambiente

Um ambiente é uma instalação completa e independente do sistema: aplicação, banco, configuração. Ter só produção significa que a primeira vez que o código encontra dados reais, rede real e configuração real é com o cliente na frente.

Cada ambiente responde a uma pergunta diferente:

| Ambiente | Pergunta que responde | Dados | Quem usa |
|---|---|---|---|
| **development** | "Integrou sem quebrar?" | descartáveis, gerados | quem desenvolve |
| **staging** | "Comporta como vai comportar em produção?" | espelho anonimizado de produção | quem valida antes de liberar |
| **production** | — | reais | quem usa o produto |

O valor do staging depende inteiramente de ele **parecer** com produção: mesma versão de banco, mesmo motor, mesma configuração, volume de dados parecido. Um staging com SQLite validando uma produção com MySQL não valida nada, só dá uma sensação boa antes do problema.

## Os ambientes do Florescer

| Environment | Alimentado por | Tag da imagem | Aprovação |
|---|---|---|---|
| `development` | push em `develop` | `:dev` | automática |
| `staging` | push em `release/*` | `:x.y.z-rc` | automática |
| `production` | tag `v*.*.*` na `main` | `:x.y.z` | **manual** |

O `rc` de `1.2.0-rc` significa *release candidate*: é o que vai virar a 1.2.0 se passar em staging.

**Estado atual, para não haver ilusão**: os três ambientes existem no pipeline e publicam imagens versionadas no GitHub Container Registry, mas **ainda não há servidor de destino**. O job `deploy` do `cd.yml` é um placeholder explícito. Quando existir infraestrutura, é aquele job que passa a puxar a imagem já publicada e rodar o smoke test. Nada além dele muda.

## Configuração por ambiente: nunca no código

O mesmo binário roda nos três ambientes. O que muda é a configuração injetada por fora, via variável de ambiente.

É o princípio de config do [Twelve-Factor App](https://12factor.net/config): se você precisa recompilar para trocar de ambiente, o artefato testado não é o artefato entregue.

No Florescer isso aparece assim:

```yaml
# application.yml
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
jwt:
  private-key: ${RSA_PRIVATE_KEY:classpath:chaves/app.key}
```

`${VAR}` sem default é obrigatório: falta dele, a aplicação não sobe. Isso é proposital. Falhar no boot é infinitamente melhor que subir com uma configuração de mentira e descobrir depois.

`${VAR:default}` usa o default quando a variável não existe. Aqui o default aponta para um arquivo local que só existe na máquina do desenvolvedor, então em produção a variável real é obrigatória na prática.

Os segredos por ambiente ficam em **GitHub Environment secrets**, que só são expostos aos jobs vinculados àquele environment. O segredo de produção nunca é visível para um job de development.

## Promoção de artefato

A regra: **a mesma imagem que passou em staging é a que vai para produção.** Reconstruir para promover invalida o teste, porque o binário que rodou em staging deixa de ser o binário que roda em produção.

Por isso o `cd.yml` publica cada imagem com duas tags:

- `ghcr.io/.../auth-service:1.2.0` — o apelido legível
- `ghcr.io/.../auth-service:<sha do commit>` — o identificador imutável

A tag de sha é a que garante rastreabilidade: dado um container rodando, dá para chegar no commit exato que o gerou.

## O caminho completo de uma mudança

```
commit na feature branch
   └─ PR para develop ─── CI: build + testes + cobertura + secret scan
        └─ merge em develop ─── CD: publica :dev ─── environment development
             └─ branch release/1.2.0 ─── CD: publica :1.2.0-rc ─── environment staging
                  └─ validação em staging
                       └─ merge na main + tag v1.2.0
                            └─ CD: publica :1.2.0 ─── environment production
                                 └─ PAUSA esperando aprovação humana
                                      └─ deploy
```

O único ponto onde um humano é obrigatório é a aprovação de produção. Todo o resto é automático, e é isso que torna o processo repetível: não existe passo manual para alguém esquecer.

## Rollback

A pergunta que separa quem já operou produção de quem não operou é: **quanto tempo leva para desfazer?**

Com imagens versionadas, o rollback de aplicação é trocar a tag para a versão anterior e reiniciar. É rápido justamente porque a imagem antiga continua existindo no registry.

**Migração de banco é o que complica.** Se a versão 1.2.0 removeu uma coluna, voltar para a 1.1.0 encontra um banco onde a coluna não existe mais. A regra prática, e que vale como resposta em entrevista:

> Migração de banco deve ser **aditiva e compatível com a versão anterior**. Adicionar coluna é seguro. Remover ou renomear não é, a menos que seja feito em duas etapas: primeiro o código para de usar (e essa versão vai a produção), depois a coluna é removida numa versão seguinte.

O mesmo raciocínio vale para contrato de API: adicionar campo é compatível; remover ou mudar o tipo de um campo existente quebra quem consome.

Duas técnicas que reduzem o risco do deploy em si:

- **Blue/green**: duas instalações completas. A nova recebe o deploy, é verificada, e o tráfego é chaveado de uma vez. Rollback é chavear de volta.
- **Canary**: a versão nova recebe uma fatia pequena do tráfego (5%), as métricas de erro são observadas, e a fatia cresce se estiver saudável.

Nenhuma das duas está implementada aqui. Ambas dependem de infraestrutura que este projeto ainda não tem, e prometer o que não existe é pior que não ter.

## Smoke test pós-deploy

Depois que o deploy termina, o pipeline precisa **verificar** que o sistema está de pé, em vez de descobrir pelo cliente. O mínimo:

1. Endpoint de health responde 200.
2. O fluxo principal funciona ponta a ponta (aqui: login devolve token, listagem de produtos responde).
3. A versão que respondeu é a versão que acabou de subir.

Esse é o próximo passo natural quando o destino de deploy existir, e cabe no mesmo job `deploy` do `cd.yml`.

## Vocabulário

- **Artefato**: o que o build produz. Aqui, a imagem Docker.
- **Registry**: onde as imagens ficam guardadas. Aqui, GHCR (GitHub Container Registry).
- **Imutabilidade**: uma imagem publicada nunca muda. Corrigir gera imagem nova, não sobrescreve.
- **Idempotência**: rodar o deploy duas vezes tem o mesmo efeito de rodar uma vez.
- **Health check**: endpoint que responde se a aplicação está viva (*liveness*) e pronta para receber tráfego (*readiness*). São coisas diferentes: uma aplicação pode estar viva mas ainda não pronta, por exemplo enquanto espera o banco.
- **Drift de configuração**: quando ambientes que deveriam ser iguais divergem silenciosamente. É a causa raiz clássica do "só quebra em produção".
