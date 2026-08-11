# product-service

Catálogo: produtos e suas imagens. Porta 8081, PostgreSQL.

Para visão geral, arquitetura e como subir tudo, ver o [README da raiz](../README.md).

## Endpoints

| Método | Rota | Quem pode | O que faz |
|---|---|---|---|
| `GET` | `/v1/product` | qualquer um | lista paginada |
| `GET` | `/v1/product/{id}` | qualquer um | detalhe |
| `POST` | `/v1/product` | ADMIN | cria, com imagem obrigatória |
| `PATCH` | `/v1/product/{id}` | ADMIN | altera só o que foi enviado |
| `DELETE` | `/v1/product/{id}` | ADMIN | remove |
| `GET` | `/uploads/**` | qualquer um | serve as imagens |
| `GET` | `/actuator/health` | qualquer um | UP ou DOWN, sem detalhe |

Leitura pública porque a vitrine precisa funcionar para quem ainda não tem conta. Escrita restrita a ADMIN, verificada por método com `@PreAuthorize` e coberta por uma matriz de 14 casos.

## Decisões que não são óbvias

**O nome do arquivo enviado é descartado.** A imagem é gravada como `UUID` mais a extensão derivada dos **bytes reais** do arquivo, não do `Content-Type` declarado nem do nome original. Content-Type é escolhido por quem envia, e nome original com `../` sai do diretório.

**Preço é `BigDecimal` com escala fixa.** `Double` é ponto flutuante binário e não representa decimal exatamente: somar itens acumula erro e comparar por igualdade deixa de ser confiável. Com dinheiro isso vira diferença de centavos que ninguém explica.

**Escrita em disco acontece depois do commit.** Gravar e apagar arquivo dentro da transação deixa o disco fora de sincronia quando o banco desfaz: sobra arquivo órfão ou some a imagem de um produto que continua existindo.

**PATCH distingue campo ausente de campo nulo.** Só o que veio é alterado. Confundir os dois apagaria dado que ninguém pediu para apagar.

**A paginação tem teto e lista de campos permitidos.** `size` acima do limite é recusado, não cortado em silêncio, e `sort` só aceita campos conhecidos. Sem isso, uma rota pública aceita `size=1000000` e ordenação por campo arbitrário.

**Toda escrita deixa rastro** de quem fez e em qual produto, com o autor pseudonimizado.

## Configuração

| Variável | Obrigatória | O que é |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | sim | acesso ao PostgreSQL |
| `JWT_JWKS_URI` | uma das duas | endpoint de chaves do auth-service; permite rotação sem redeploy |
| `RSA_PUBLIC_KEY` | uma das duas | chave fixa em PEM; sem rotação sem redeploy |
| `JWT_ISSUER` | não | emissor esperado, padrão `auth-service` |
| `UPLOADS_DIR` | não | onde as imagens são gravadas |
| `CORS_ALLOWED_ORIGINS` | não | origens do navegador |
| `LOG_PSEUDONYM_SALT` | não | salt do pseudônimo de auditoria |

`JWT_JWKS_URI` tem precedência quando as duas estão preenchidas. Sem nenhuma das duas o serviço não sobe: sem chave não há o que validar.

## Testes

```bash
./mvnw verify
```

Sobe um PostgreSQL real em container. `verify` inclui o piso de cobertura: 75% de linha, 55% de ramo.
