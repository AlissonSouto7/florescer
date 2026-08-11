# Catálogo de produtos

CRUD de produtos. Leitura pública, escrita restrita a ADMIN.

**Onde fica**: `product-service`, porta 8081, PostgreSQL 16.
**Status**: funcional, coberto por testes.
**Última revisão**: 11/08/2026.

## Endpoints

| Método | Rota | Quem pode | O que faz |
|---|---|---|---|
| `GET` | `/v1/product` | qualquer um | lista paginada |
| `GET` | `/v1/product/{id}` | qualquer um | detalhe |
| `POST` | `/v1/product` | ADMIN | cria, imagem obrigatória |
| `PATCH` | `/v1/product/{id}` | ADMIN | altera só o que foi enviado |
| `DELETE` | `/v1/product/{id}` | ADMIN | remove |
| `GET` | `/actuator/health` | qualquer um | UP ou DOWN |

Leitura pública porque a vitrine precisa funcionar para quem ainda não tem conta. Escrita verificada por método, com `@PreAuthorize("hasRole('ADMIN')")`.

## Camadas

| Camada | Arquivo |
|---|---|
| Controller | `api/controller/impl/ProductControllerImpl.java` |
| Contrato | `api/controller/ProductController.java` |
| DTOs | `api/dto/request/`, `api/dto/response/` |
| Mapper | `api/mapper/ProductMapper.java` |
| Paginação | `api/utils/PageableFactory.java` |
| Serviço | `domain/service/impl/ProductServiceImpl.java` |
| Comandos de domínio | `domain/model/NewProduct.java`, `ProductChanges.java` |
| Entidade | `domain/entity/Product.java` |
| Repositório | `infra/repository/ProductRepositoryImpl.java` |
| Erros | `domain/exception/GlobalExceptionHandler.java` |
| Schema | `resources/db/migration/V1__create_product_table.sql` |

O domínio não importa DTO da camada `api`: o serviço recebe `NewProduct` e `ProductChanges`. Sem isso, mudar o formato da API forçaria mudança na regra de negócio.

## Regras e por quê

**Preço é `BigDecimal` com `numeric(10,2)`.** `Double` é ponto flutuante binário e não representa decimal exatamente: somar itens acumula erro e comparar por igualdade deixa de ser confiável. Com dinheiro isso vira diferença de centavos que ninguém consegue explicar.

**PATCH distingue campo ausente de campo nulo.** Só o que veio é alterado. Confundir os dois faria quem edita apenas o preço perder a descrição.

**PATCH sem nenhum campo e sem imagem é recusado.** Aceitar em silêncio responderia sucesso sem ter feito nada, e quem chamou não teria como saber que o pedido se perdeu.

**Paginação tem teto e lista de campos permitidos.** `size` acima do limite é **recusado**, não cortado em silêncio, porque cortar entrega uma página diferente da pedida sem avisar. `sort` só aceita campos conhecidos: sem isso, uma rota pública ordena por qualquer coluna, inclusive as que não deveriam ser observáveis.

**Escrita em disco acontece depois do commit.** Gravar ou apagar arquivo dentro da transação deixa o disco fora de sincronia quando o banco desfaz. No PATCH a imagem nova é gravada **antes** de trocar a referência: se a escrita falhar, o produto continua com a que tinha. A antiga só é apagada depois do commit.

**Toda escrita registra quem fez e em qual produto**, com o autor pseudonimizado. No PATCH vão os nomes dos campos alterados, nunca os valores.

## Achados de segurança

### Corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| P-1 | alto | path traversal: nome do arquivo enviado usado como está, com `createDirectories` no caminho derivado | nome descartado, arquivo vira UUID, caminho normalizado e confinado ao diretório |
| P-2 | alto | upload validado só pelo `Content-Type` declarado, com extensão preservada e servida estaticamente: XSS armazenado | tipo detectado pelos bytes reais, extensão derivada do tipo detectado |
| P-3 | alto | Bean Validation nunca executava: `@Valid` numa `String` com `ObjectMapper` manual | part tipado, validação viva, handler de erro alcançável |
| P-4 | médio | imagens inacessíveis: URL gerada em `/images/`, handler em `/uploads/**`, nenhum em `permitAll` | caminho unificado e liberado para leitura |
| P-5 | médio | erro de cliente virando 500 (corpo malformado, método errado, arquivo ausente) | handlers específicos por exceção |
| P-6 | médio | `size` sem teto e `sort` sem lista permitida numa rota pública | teto de 50 com recusa explícita, campos em allow-list |
| P-7 | médio | I/O de disco dentro da transação | `TransactionSynchronization.afterCommit` |
| P-8 | médio | sem CORS | origens por configuração |
| P-9 | médio | sessão não era STATELESS | `SessionCreationPolicy.STATELESS` |
| P-10 | baixo | PATCH exigia imagem, contrariando a documentação e o serviço | `required = false` |
| P-11 | baixo | arquivo ausente respondia 500 em vez de 404 | handler de `NoResourceFoundException` |

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| P-12 | baixo | `GlobalExceptionHandler.java:107` registra `ex.getResourcePath()`, que vem da URL do cliente: mesma forma de log injection que o CodeQL apontou no `RateLimitFilter` | **não verificado** se é explorável. O `StrictHttpFirewall` do Spring Security rejeita caracteres de controle na URL por padrão, o que provavelmente neutraliza o caso, mas isso é hipótese e não medição. Issue #52 diz qual teste resolve a dúvida |
| P-13 | baixo | as imagens são servidas pela mesma origem da API | separar em domínio próprio (issue #23) impede que um arquivo enviado por alguém rode no contexto da aplicação. Depende de infraestrutura que ainda não existe |
| P-14 | baixo | rota no singular (`/v1/product`) enquanto a convenção REST pede a coleção no plural | mudar quebra o frontend; corrigir exige versionar ou migrar as duas pontas juntas |

### Verificado e OK

- **Nenhuma query é montada por concatenação.** Sem `whereRaw` ou equivalente; tudo passa por Spring Data ou por parâmetro.
- **O `@PreAuthorize` está nos três verbos de escrita** e a matriz de 14 casos prova que cada um recusa anônimo com 401 e BASIC com 403.
- **Remover o `@PreAuthorize` faz a suíte falhar.** Testado de propósito: o `DELETE` sem a anotação deixou um usuário BASIC alcançar o método, e a matriz acusou.
- **A imagem não é apagada quando a transação desfaz.** `TransactionalFileIoTest` cobre.

## Testes

| Teste | Risco que protege |
|---|---|
| `AuthorizationMatrixTest` (14) | endpoint aberto para quem não devia; 401 e 403 trocados |
| `ImageStorageServiceTest` (5) | path traversal; arquivo que não é imagem aceito |
| `TransactionalFileIoTest` (4) | arquivo órfão ou imagem perdida quando o banco desfaz |
| `PartialPatchTest` (4) | PATCH apagando campo que ninguém enviou |
| `PaginationLimitsTest` (9) | `size` ilimitado e ordenação por campo arbitrário |
| `MoneyPrecisionTest` (3) | preço perdendo centavos |
| `ProductValidationTest` (5) | entrada inválida chegando ao banco |
| `ProductImageUrlTest` (4) | URL de imagem divergente entre listagem e detalhe |
| `ErrorResponseTest` (7) | erro de cliente virando 500 |
| `JwtKeyRotationTest` (3) | chave antiga continuando a valer após rotação |
| `JwksRotationTest` (2) | rotação exigindo reinício do serviço |
| `CorsTest` (4) | API deixando de responder ao frontend, ou respondendo a qualquer origem |
| `WriteAuditTest` (3) | escrita sem rastro de autor; e-mail do autor em texto puro |
| `HealthEndpointTest` (12) | Actuator expondo configuração interna |
| `OpenApiContractTest` (4) | documentação divergindo do que a API faz |

**84 testes, cobertura medida em 83% de linha e 66% de ramo** (11/08/2026).

### O que NÃO está coberto

- **Ramos de erro internos**: falha ao gravar imagem com disco cheio, e o `catch` do repositório. São os candidatos naturais para o próximo aumento do piso de ramo.
- **Concorrência em escrita de produto**: dois PATCH simultâneos no mesmo produto não têm teste. O risco aqui é menor que no registro (não há unicidade em jogo), mas last-write-wins é o comportamento atual e ninguém decidiu isso conscientemente.
- **Upload de arquivo grande de verdade**: o limite de 10MB é configurado e o handler existe, mas nenhum teste envia um arquivo acima do teto.
- **`api/dto/response` aparece com 21% de cobertura**: é ruído de métrica, não risco. São records, e o JaCoCo conta como não coberto todo acessor que nenhum teste chama diretamente, mesmo quando o objeto é serializado e verificado via JSON.

## Como verificar em produção

```bash
# Pronto para receber tráfego?
curl -s http://HOST:8081/actuator/health/readiness

# A vitrine responde para quem não tem conta?
curl -s "http://HOST:8081/v1/product?page=0&size=5" | jq '.content | length'

# O teto de página é respeitado? Deve recusar, não devolver mil itens.
curl -s -o /dev/null -w "%{http_code}\n" "http://HOST:8081/v1/product?size=1000"
```

```sql
-- Produtos apontando para imagem inexistente exigem checar o disco;
-- pelo banco, o que dá para ver é referência vazia:
SELECT id, name FROM tb_products WHERE image_path IS NULL OR image_path = '';

-- Preço fora da escala esperada:
SELECT id, name, price FROM tb_products WHERE price <= 0;
```

Quem alterou um produto, pelo log:

```bash
grep 'Produto alterado' /var/log/florescer/product.log | grep 'productId=UUID-AQUI'
```

## Dívida conhecida

- Rota no singular (P-14).
- Sem histórico de alterações no banco: o log responde "quem mudou", mas não "qual era o valor antes".
- `isSameImage` compara conteúdo carregando o arquivo; para imagens grandes, hash em streaming seria melhor.

## Histórico

| Data | O que mudou |
|---|---|
| 11/08/2026 | matriz de autorização, patch parcial, auditoria de escrita, health, JWKS |
| 10/08/2026 | Flyway, `BigDecimal`, I/O após commit, camadas do domínio |
| 09/08/2026 | upload endurecido, validação viva, CORS, paginação defensiva |
