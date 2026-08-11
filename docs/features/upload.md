# Upload de imagens

Recebe, valida e serve as fotos dos produtos.

**Onde fica**: `product-service`. Escrita junto com o produto (`POST` e `PATCH` de `/v1/product`), leitura em `/uploads/**`.
**Status**: funcional e endurecido, com uma limitação de arquitetura em aberto.
**Última revisão**: 11/08/2026.

## Por que este documento existe separado

Upload é a superfície mais perigosa de qualquer API. É o único ponto onde alguém de fora coloca um **arquivo** dentro do servidor, e cada decisão aqui tem consequência de segurança: onde grava, com que nome, o que aceita e como serve depois.

## Camadas

| Camada | Arquivo |
|---|---|
| Gravação e validação | `infra/storage/ImageStorageService.java` |
| Remoção após commit | `infra/storage/FileCleanupOnCommit.java` |
| Rota de leitura | `config/StaticResourceConfig.java` |
| Limite de tamanho | `application.yml`, `spring.servlet.multipart` |

## O caminho de um arquivo

1. Chega como `MultipartFile` no `POST` ou `PATCH`.
2. **O nome original é descartado**, inteiro.
3. Os **primeiros bytes** são lidos e comparados com as assinaturas de JPEG, PNG e WebP.
4. Se não casar com nenhuma, a requisição é recusada.
5. O arquivo é gravado como `UUID` mais a extensão **derivada do tipo detectado**.
6. O caminho final é normalizado e conferido: precisa começar dentro do diretório configurado.
7. A referência vai para o banco; o arquivo antigo, quando houver, só é apagado depois do commit.

## Regras e por quê

**O nome enviado nunca é usado.** `getOriginalFilename()` é escolhido por quem envia. Um nome como `../../../etc/cron.d/tarefa` sai do diretório, e o código antigo ainda chamava `createDirectories` no caminho derivado, ou seja, criava a pasta necessária para o ataque funcionar.

**A extensão vem dos bytes, não do que foi declarado.** `Content-Type` é preenchido pelo cliente. Aceitar `image/png` sem olhar o conteúdo permite enviar um `.html` com script dentro, que depois é servido pela aplicação e roda no navegador de quem abrir: XSS armazenado, com o arquivo hospedado pelo próprio sistema.

**O diretório é configurável e fica fora do artefato.** Dentro do jar, toda imagem sumiria no próximo deploy e o banco ficaria apontando para arquivo inexistente. No compose ele é um volume nomeado.

**Apagar arquivo só depois do commit.** Se o banco desfizer a transação, o produto continua existindo e precisa da imagem. Apagar antes deixaria uma referência apontando para o vazio, e nenhuma forma de recuperar.

**No PATCH, gravar a nova antes de trocar a referência.** Se a escrita falhar, o produto continua com a imagem que tinha. A ordem inversa perderia as duas.

**Limite de 10MB por arquivo e por requisição.** Sem teto, uma rota autenticada vira um jeito barato de encher o disco.

## Achados de segurança

### Corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| U-1 | alto | path traversal pelo nome do arquivo, agravado por `createDirectories` no caminho derivado | nome descartado, caminho normalizado e confinado |
| U-2 | alto | tipo aceito pelo `Content-Type` declarado, extensão preservada, arquivo servido estaticamente: XSS armazenado | detecção por magic bytes, extensão derivada do tipo real |
| U-3 | médio | diretório fixo e relativo, dentro do artefato | configurável por `UPLOADS_DIR`, volume no compose |
| U-4 | médio | remoção do arquivo dentro da transação | `afterCommit` |
| U-5 | médio | URL gerada em `/images/` e handler em `/uploads/**`: imagem nunca carregava | caminho unificado |
| U-6 | médio | `/uploads/**` não estava em `permitAll`: vitrine anônima sem foto | leitura liberada |
| U-7 | baixo | arquivo inexistente respondia 500 | 404 pelo handler de `NoResourceFoundException` |
| U-8 | baixo | separador `\` do Windows não tratado na verificação de caminho | normalização cobre os dois |

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| U-9 | médio | as imagens são servidas **pela mesma origem da API**. Um arquivo enviado por alguém e servido em `florescer.com/uploads/x` compartilha origem com a aplicação, então uma falha futura de tipo ou de escape roda no contexto dela, com acesso ao `localStorage` | corrigir é servir de um domínio separado (issue #23). Depende de infraestrutura que ainda não existe. Enquanto isso, a defesa é a detecção por magic bytes e o `X-Content-Type-Options: nosniff` do nginx |
| U-10 | baixo | não há varredura antivírus nem limite de dimensão da imagem | um PNG válido de 20000x20000 passa no teste de tipo e consome memória ao ser processado. Hoje nada processa a imagem além de gravá-la, então o risco é de disco, coberto pelo limite de 10MB |

### Verificado e OK

- **`..` no nome não escapa do diretório.** Testado com `../../../etc/passwd`; o nome inteiro é descartado antes de qualquer uso.
- **HTML disfarçado de imagem é recusado.** `ImageStorageServiceTest` envia `<script>alert(document.cookie)</script>` com `Content-Type: image/png` e a gravação falha.
- **A imagem sobrevive a uma transação que desfaz** e o arquivo órfão não fica para trás quando ela confirma. `TransactionalFileIoTest` cobre os dois sentidos.
- **A URL devolvida é a mesma na listagem e no detalhe.** Contrato único, verificado por teste, depois de um período em que os dois endpoints divergiam.

## Testes

| Teste | Risco que protege |
|---|---|
| `ImageStorageServiceTest` (5) | traversal; arquivo que não é imagem; nome original influenciando o destino |
| `TransactionalFileIoTest` (4) | arquivo órfão após rollback; imagem apagada de produto que sobreviveu |
| `ProductImageUrlTest` (4) | URL divergente entre endpoints; imagem inalcançável para anônimo |

### O que NÃO está coberto

- **Arquivo acima do limite de 10MB**: o teto está configurado e o handler existe, mas nenhum teste envia um arquivo grande de verdade.
- **Disco cheio durante a gravação**: o `catch` existe e não tem caso próprio. É código de recuperação sem teste, que é o que mais falha quando é acionado.
- **Uploads simultâneos do mesmo produto**: dois PATCH concorrentes trocando a imagem não foram exercitados.
- **Imagem com dimensões absurdas** (U-10).

## Como verificar em produção

```bash
# A imagem de um produto é alcançável sem token?
curl -s -o /dev/null -w "%{http_code}\n" http://HOST:8081/uploads/ARQUIVO.png

# O tipo devolvido bate com o conteúdo, e o nosniff está presente?
curl -sI http://HOST:8081/uploads/ARQUIVO.png | grep -Ei 'content-type|x-content-type-options'
```

```bash
# Arquivos no diretório que nenhum produto referencia (órfãos).
# Somente leitura: lista, não apaga.
ls /var/florescer/uploads | while read f; do
  psql -U USUARIO -d dbproduct -tAc \
    "SELECT 1 FROM tb_products WHERE image_path LIKE '%$f'" | grep -q 1 || echo "órfão: $f"
done
```

```sql
-- Produtos sem referência de imagem.
SELECT id, name FROM tb_products WHERE image_path IS NULL OR image_path = '';
```

## Dívida conhecida

- Domínio separado para servir os arquivos (U-9).
- `isSameImage` carrega o arquivo inteiro para comparar; hash em streaming seria melhor para imagens grandes.
- Nenhuma imagem é redimensionada nem otimizada: a vitrine carrega o arquivo original.

## Histórico

| Data | O que mudou |
|---|---|
| 11/08/2026 | volume nomeado no compose, `nosniff` no nginx |
| 10/08/2026 | I/O de disco movida para depois do commit |
| 09/08/2026 | nome descartado, detecção por magic bytes, caminho confinado, rota unificada |
