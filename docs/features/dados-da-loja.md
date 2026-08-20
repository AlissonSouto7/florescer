# Dados da loja

Os quatro dados que a vendedora muda sem chamar ninguém: o WhatsApp que recebe os pedidos, a cidade onde entrega, o @ do Instagram e o horário de atendimento.

**Onde fica**: tabela `shop_settings` no product-service; tela em `/admin/configuracoes`; leitura no rodapé de toda página e no botão de comprar.
**Status**: funcional.
**Última revisão**: 20/08/2026.

## Por que isto existe

O número de WhatsApp vivia numa variável de ambiente do servidor e o rodapé tinha texto escrito no código. Trocar qualquer um dos dois significava editar arquivo e reiniciar container, coisa que a vendedora não faz.

Número desatualizado é o pior defeito possível neste sistema: o pedido não chega em ninguém e **nada na tela avisa**. Ela continua achando que está vendendo.

## Arquitetura

| Camada | Arquivo |
|---|---|
| migration | `product-service/src/main/resources/db/migration/V3__create_shop_settings.sql` |
| entidade | `domain/entity/ShopSettings.java` |
| regra | `domain/service/impl/ShopSettingsServiceImpl.java` |
| erro de validação | `domain/exception/custom/InvalidSettingsException.java` |
| entrada/saída | `api/dto/request/ShopSettingsRequest.java`, `api/dto/response/ShopSettingsResponse.java` |
| controller | `api/controller/impl/ShopSettingsControllerImpl.java` |
| cliente web | `florescer-web/lib/loja.ts` |
| formulário | `florescer-web/components/FormularioDaLoja.tsx` |
| tela | `florescer-web/app/admin/configuracoes/page.tsx` |
| consumo | `florescer-web/app/layout.tsx` (rodapé), `lib/whatsapp.ts` (botão) |

| Método | Rota | Quem pode | O que faz |
|---|---|---|---|
| GET | `/v1/settings` | qualquer um | devolve os quatro campos |
| PUT | `/v1/settings` | ADMIN | grava os quatro, já normalizados |

A leitura é pública porque a vitrine é pública: o rodapé e o botão de comprar precisam dela em toda página, inclusive para quem nunca fez login. A escrita é `@PreAuthorize("hasRole('ADMIN')")`.

## Regras de negócio

**Uma linha só, com o id preso em 1 por um `CHECK`.** Sem essa trava, um defeito criaria uma segunda linha e a pergunta "qual das duas vale?" não teria resposta na hora da leitura. A linha nasce com a migration; se ela sumir, a aplicação levanta `IllegalStateException` em vez de fingir que a loja não tem dados.

**Todo campo é nulo por opção.** Uma loja que acabou de abrir não tem Instagram, e obrigar um valor faria alguém inventar um. Texto inventado no rodapé é idêntico a texto verdadeiro para quem lê.

**Campo em branco vira nulo, não string vazia.** A diferença aparece na tela: nulo some do rodapé, vazio deixaria um rótulo pendurado sem valor ao lado.

**A normalização mora no service, nunca no formulário.** Quem chama a API direto não passa pelo formulário. Se a limpeza vivesse na tela, um `curl` gravaria `@loja` e o rodapé montaria `instagram.com/@loja`, que não abre o perfil de ninguém.

| O que a pessoa digita | O que é gravado |
|---|---|
| `+55 (73) 99814-9668` | `5573998149668` |
| `@florescer.plantas` | `florescer.plantas` |
| `https://www.instagram.com/florescer.plantas/` | `florescer.plantas` |
| `   ` | nulo |

**O telefone é validado depois de limpo, entre 10 e 15 dígitos.** Dez é um fixo com DDD, quinze é o teto do padrão E.164. Apagar o número é permitido: sem número o botão de comprar some, o que é melhor que um botão levando a lugar nenhum.

A regra existe em três camadas, e cada uma cobre o que a anterior não alcança:

| Onde | O que checa | Por que ali |
|---|---|---|
| DTO (`@Pattern`, `@Size`) | só os caracteres que um telefone pode ter (`0-9 ( ) + - . espaço`) e o tamanho do texto | é a única checagem que pode rodar **antes** da limpeza sem recusar formato humano |
| service | 10 a 15 dígitos, depois de tirar a pontuação | é onde a regra de negócio de verdade mora, e vale também para quem chama a API sem passar pela tela |
| banco (`CHECK`) | `^[0-9]{10,15}$` no que for gravado | o dado continua válido mesmo que alguém escreva direto no banco |

**O rodapé nunca derruba a loja.** Se a API de configuração falhar, `buscarDadosDaLoja` devolve a loja vazia e o rodapé fica mais curto. Vitrine sem rodapé vende; tela de erro, não.

## Achados

### Corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| C-1 | alto | a regra de 10 a 15 dígitos era um `@Pattern` no DTO, e anotação roda **antes** da limpeza: `+55 (73) 99814-9668` era recusado, ou seja, exatamente o formato que as pessoas usam | a contagem de dígitos foi para o service, depois de tirar a pontuação, com mensagem por campo para o aviso aparecer ao lado do campo certo. No DTO ficou só a checagem de caracteres, que não depende da limpeza |
| C-2 | médio | o botão dizia "esta planta está indisponível" quando o problema era a loja não ter número configurado. Mentira sobre a planta: o visitante vai embora achando que esgotou | os dois casos passaram a ser distinguidos; `temNumero` e `podeComprar` são funções separadas |
| C-6 | alto | a variável `WHATSAPP_NUMBER` continuou declarada nos dois composes depois que ninguém mais a lia, e no de produção ela era **obrigatória** (`${WHATSAPP_NUMBER:?...}`). Numa máquina de deploy sem essa linha no `.env`, a stack inteira recusava subir por causa de um valor que não faz mais nada | removida dos dois composes, do `.env.example`, do `Dockerfile` e do README. Provado antes e depois: `docker compose -f docker-compose.prod.yml config` com um `.env` sem a variável saía com erro antes, e passa agora |
| C-3 | baixo | o rodapé tinha cache de 60 segundos, então ela salvava, abria a loja, via o número velho e concluía que não tinha salvo. Medido: até 24 segundos de defasagem | cache removido. O custo foi medido antes: 162 bytes e cerca de 17 ms por página, numa chamada que não sai da máquina |

### Verificado e OK

- **Escalação de privilégio não se aplica**: não há campo de papel nem de permissão nesta tabela. O pior que um ADMIN faz é apagar o próprio número.
- **Mass assignment**: o PUT recebe um record com quatro campos declarados. Campo extra no JSON é ignorado pelo Jackson, não existe `fill()` nem `all()`.
- **Injeção de SQL**: tudo passa por JPA com parâmetro; nenhum `nativeQuery` com concatenação.
- **XSS no rodapé**: o React renderiza como texto. Um `<script>` gravado na cidade aparece escrito na tela, não executa. Verificado com round-trip pela API.
- **Tamanho de campo**: `@Size` no DTO recusa texto maior que a coluna, em vez de estourar no banco com erro 500.
- **Segredo**: nenhum dos quatro campos é secreto. O número de WhatsApp é justamente o que a loja quer publicar.

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| C-4 | baixo | não há registro de quem mudou o quê nem quando, além do `updated_at` | uma pessoa só usa a tela; o rastro vira necessário quando houver mais de um acesso ADMIN |
| C-5 | baixo | duas edições simultâneas sobrescrevem uma à outra, sem versão nem `lockForUpdate` | são quatro campos editados por uma pessoa só, a partir de um formulário que carrega o estado atual; o dano possível é ela reescrever o que acabou de gravar |

## Testes

**Backend**: 24 casos em `product-service/src/test/java/com/florescer/product/api/ShopSettingsTest.java`, contra Postgres real via Testcontainers, dentro dos 125 testes do serviço.

```bash
cd product-service && ./mvnw verify     # inclui o gate do JaCoCo
cd florescer-web && npm run test:coverage
```

| Grupo | Casos | Risco que protege |
|---|---|---|
| autorização | 5 | leitura fechando e derrubando a vitrine; PUT sem token; conta comum autenticada alterando a loja; dado interno vazando na resposta pública |
| telefone | 9 | formato humano recusado (C-1); número curto demais gravado e levando a `wa.me` quebrado; mensagem de erro escrita para programador; apagar o número deixando de ser permitido |
| Instagram | 6 | perfil guardado com `@` ou URL inteira, montando link que não abre |
| texto | 3 | branco virando rótulo vazio no rodapé; espaço sobrando; texto maior que a coluna |
| conteúdo | 1 | texto chegando corrompido ou resposta deixando de ser JSON |

**Frontend**: 40 casos, em `lib/loja.test.ts` (12), `components/FormularioDaLoja.test.tsx` (14) e `lib/whatsapp.test.ts` (14), dentro dos 249 do frontend. Cobrem a API fora do ar não derrubando a loja, o aviso do backend chegando ao campo certo, sessão vencida levando ao login sem perder o que foi digitado, o botão travando durante o envio para dois cliques não virarem dois salvamentos, e o link de compra sobrevivendo a acento, `&` e `#` no nome da planta.

Medido em 20/08/2026: `lib/loja.ts` e `lib/whatsapp.ts` com 100% de linhas, ramos e funções; `FormularioDaLoja.tsx` com 31 de 32 linhas e 28 de 32 ramos; `ShopSettingsServiceImpl` com 96,7% de linhas e 87,5% de ramos.

### Prova de que os testes não são vacuosos

Teste que passa de primeira é suspeito, então cada regra foi quebrada de propósito, uma por vez, com a suíte rodando entre cada quebra.

**Backend: 6 mutações, 6 acusadas.** Tirar a limpeza do telefone derruba 3 casos de `limpaONumero`; tirar a normalização do Instagram derruba 5 de `normalizaOInstagram`; tirar o `@PreAuthorize` do PUT é pego por `contaComumNaoAltera`.

**Frontend: 18 mutações, 16 acusadas.** Das duas restantes, uma é mutante equivalente (trocar `return LOJA_VAZIA` por `throw` cai no `catch` logo abaixo, e o resultado visível é o mesmo) e a outra era buraco de verdade: nada impedia o rodapé de voltar a guardar em cache, que é exatamente o C-3. Virou teste, com vermelho antes (`expected undefined to be 'no-store'`) e verde depois.

A primeira rodada do backend deu "6 de 6 escaparam", inclusive em mutações impossíveis de passar despercebidas, e o resultado era do script, não dos testes: sem achar o `mvnw.cmd`, ele lia um relatório verde antigo em `target/surefire-reports`. O script passou a apagar o relatório antes de cada rodada e a tratar a ausência dele como erro. Fica registrado porque o modo de falha é traiçoeiro: um verificador quebrado aprova tudo, e parece rigor.

### O que NÃO está coberto

- **Concorrência** (C-5): não há teste de duas gravações simultâneas.
- **A tela `/admin/configuracoes`**: é um componente de servidor que busca e passa adiante; o formulário dentro dela tem teste próprio.
- **O rodapé renderizado**: `app/layout.tsx` entra no relatório com zero, como as demais páginas.

## Verificado no navegador

Em 20/08/2026, com a stack de pé: preencher o formulário como ela preencheria, com `+55 (73) 99814-9668` no telefone e a URL completa do Instagram, salvar, e conferir na vitrine.

| Verificação | Resultado |
|---|---|
| telefone gravado | `5573998149668` |
| Instagram gravado | `florescer.plantas` |
| rodapé, entrega | "ENTREGA EM Itabuna e região, BA" |
| rodapé, horário | "ATENDE Segunda a sábado, das 8h às 18h" |
| rodapé, Instagram | `@florescer.plantas`, link abrindo em aba nova |
| botão de comprar | aponta para o número gravado |

## Como verificar em produção

```bash
# O que a loja está publicando agora
curl -s http://HOST:3000/api/settings | jq

# O número está só com dígitos? (qualquer coisa diferente quebra o wa.me)
curl -s http://HOST:3000/api/settings | jq -r '.whatsappNumber' | grep -qE '^[0-9]{10,15}$' && echo ok || echo PROBLEMA

# Escrever sem token tem que ser recusado
curl -s -o /dev/null -w '%{http_code}\n' -X PUT http://HOST:3000/api/settings \
  -H 'Content-Type: application/json' -d '{}'   # espera 401
```

## Dívida conhecida

- Sem histórico de alteração (C-4) e sem controle de concorrência (C-5).
- Os textos do rodapé que **não** vêm daqui continuam no código: a descrição da loja e o aviso de direitos autorais.
- Não há e-mail nem telefone fixo. Se a loja passar a atender por outro canal, entra aqui.

## Histórico

| Data | O que mudou |
|---|---|
| 20/08/2026 | os quatro campos passaram a ser editáveis pela vendedora; o número saiu da variável de ambiente |
