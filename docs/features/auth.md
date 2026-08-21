# Autenticação

Registro, login e emissão dos tokens que o resto do sistema aceita.

**Onde fica**: `auth-service`, porta 8080, MySQL 8.4.
**Status**: funcional, coberto por testes, sem endpoint protegido próprio.
**Última revisão**: 20/08/2026.

## Endpoints

| Método | Rota | Quem pode | O que faz |
|---|---|---|---|
| `POST` | `/v1/auth/register` | qualquer um | cria conta com papel BASIC |
| `POST` | `/v1/auth/login` | qualquer um | devolve o token de acesso |
| `GET` | `/.well-known/jwks.json` | qualquer um | chave pública que valida os tokens |
| `GET` | `/actuator/health` | qualquer um | UP ou DOWN, sem detalhe |
| `GET` | `/swagger` | qualquer um | documentação interativa, **fora do ar por padrão** (`SWAGGER_ENABLED`) |

Todas públicas. A regra padrão é `anyRequest().authenticated()`, para que um endpoint novo nasça fechado em vez de aberto por esquecimento.

## Camadas

| Camada | Arquivo |
|---|---|
| Controller | `application/controller/AuthControllerImpl.java` |
| Contrato | `application/controller/AuthController.java` |
| Serviço | `application/service/AuthServiceImpl.java` |
| Identidade p/ Spring | `application/service/UserDetailsServiceImpl.java` |
| Entidade | `domain/entity/User.java`, `domain/entity/Role.java` |
| DTOs | `domain/dto/RegisterRequest.java`, `LoginRequest.java` |
| Emissão de token | `infrastructure/security/JwtServiceImpl.java` |
| Chaves e JWKS | `config/JwtConfig.java`, `application/controller/JwksController.java` |
| Rate limit | `infrastructure/ratelimit/RateLimiter.java`, `RateLimitFilter.java` |
| Pseudônimo de log | `infrastructure/logging/SensitiveData.java` |
| Correlação | `infrastructure/logging/CorrelationIdFilter.java` |
| Erros | `exception/handler/GlobalExceptionHandler.java` |
| Schema | `resources/db/migration/V1__create_user_and_role_tables.sql` |

## Regras e por quê

**Senha de 12 a 64 caracteres, sem exigir símbolo.** Segue o NIST 800-63B, que coloca comprimento acima de composição: regra de composição empurra a pessoa para `Senha@123`, que é curta e previsível. O teto de 64 não é política, é o limite prático do BCrypt, que trunca em 72 bytes; sem teto, dois textos diferentes com o mesmo prefixo dariam o mesmo hash.

**O login não valida formato de senha.** Só `@NotBlank`. Validar formato ali devolveria 400 anunciando a política a quem está tentando adivinhar, e travaria quem tem senha antiga quando a regra mudar. Senha errada é 401.

**Registro e login respondem a mesma coisa para conta existente e inexistente.** Distinguir os dois informa se um endereço tem conta, o que permite montar lista de clientes a partir de tentativas.

**Unicidade do e-mail é garantida pelo banco, não pelo código.** A consulta prévia existe para dar uma mensagem melhor no caso comum. A trava que protege o dado é a constraint `uk_users_email`.

**Rate limit antes da consulta ao banco.** Uma tentativa recusada não deve custar consulta nem verificação de BCrypt, que é cara de propósito.

**O token não é validado por este serviço.** Quem valida é o resource server do Spring, pelo `JwtDecoder`. O serviço só emite.

## Como o login é limitado, e por que não é pelo endereço

Este sistema não consegue saber de onde veio a tentativa, e as duas formas de fingir que consegue falham para lados opostos. Ambas foram medidas contra a stack de pé, em 20/08/2026:

| Como limitar | O que acontece | Medido |
|---|---|---|
| pelo endereço da conexão | o navegador fala com o site, e o site repassa: **toda tentativa do mundo chega do mesmo endereço**. Uma cota só para todos | dez `401` de um desconhecido pelo site, e em seguida `429` na senha **certa** da vendedora |
| pelo `X-Forwarded-For` | o cliente também manda esse cabeçalho, e o Next repassa sem sobrescrever: **o atacante escolhe a própria cota** | quinze tentativas trocando o valor a cada uma, **nenhum `429`** |

O que o atacante não escolhe é **a conta**. Para descobrir a senha da vendedora, é contra a conta dela que ele precisa tentar. Por isso a contagem é por conta.

E a regra que impede a proteção de virar bloqueio: **a senha é verificada primeiro, e só quem erra é recusado**. Quem acerta entra mesmo com o contador estourado. Errar a senha de alguém não pode ser uma forma de trancar essa pessoa.

O limite por origem continua no lugar, com o `ClientResolver`, por dois motivos: segura volume desatento, e passa a estar correto no dia em que houver na borda um proxy que escreva o cabeçalho, sem mudar código. Até lá, ele não é o que protege a senha.

## Achados de segurança

### Corrigidos

| id | sev | o que era | correção |
|---|---|---|---|
| A-1 | crítico | chave privada RSA versionada no repositório, assinando todos os tokens | par rotacionado, chaves por configuração, histórico purgado |
| A-2 | crítico | admin `admin@florescer.com`/`admin123` fixo no código e impresso no stdout | vem do ambiente, desligado por padrão, senha nunca registrada |
| A-3 | alto | 409 e 404 devolviam o e-mail informado, permitindo enumerar contas | resposta genérica, endereço fora do corpo |
| A-4 | alto | chaves lidas por caminho relativo do filesystem, quebrava em JAR e container | PEM por variável ou resource `classpath:`/`file:` |
| A-5 | médio | e-mail em texto puro no log INFO | pseudônimo SHA-256 com salt |
| A-6 | médio | sem CORS: o frontend não conseguia chamar a API | origens por configuração, sem curinga |
| A-7 | médio | sem rate limit em login e registro | janela deslizante por origem |
| A-8 | médio | `JwtAuthFilter` duplicava a autenticação, logava e-mail em três pontos, transformava token válido de conta ausente em erro de servidor e respondia texto puro fora do contrato JSON | filtro removido |
| A-9 | médio | corrida no registro: duas requisições simultâneas passavam pela consulta e a segunda quebrava com 500 | handler devolve o mesmo 409 do caminho normal |
| A-10 | médio | rota inexistente respondia 500 | handler de `NoResourceFoundException` |
| A-11 | baixo | `@Data` na entidade `User` colocava o hash da senha no `toString` | `@ToString.Exclude` nos campos sensíveis |
| A-16 | alto | o limite de tentativas usava o endereço da conexão, e em produção **toda tentativa chega do site**, não do navegador. Resultado: uma cota só para o mundo inteiro. Dez senhas erradas de um desconhecido trancavam a vendedora por 60 segundos, e repetir a cada minuto a mantinha fora do painel. Custa dez requisições e não exige conta nenhuma. Medido: os dez 401 pelo site, e em seguida `429` na senha **certa** | `ClientResolver` lê o `X-Forwarded-For`, mas só quando a conexão vem de um proxy declarado, e contando da direita para a esquerda, que é a parte que o cliente não escreve |
| A-17 | alto | a primeira correção do A-16 trocou um problema por outro **pior**: passar a ler o `X-Forwarded-For` tornou o limite contornável, porque **o Next repassa o cabeçalho do cliente sem sobrescrever**. Medido: quinze tentativas de login trocando o valor a cada uma, **nenhum `429`**. Adivinhar senha é pior que um bloqueio de 60 segundos | a contagem passou a ser **por conta**, que o atacante não escolhe, e com a regra que resolve o dilema: a verificação da senha vem primeiro e **só quem erra é recusado**, então a senha certa entra mesmo com o contador estourado e ninguém consegue trancar a vendedora |

### Abertos

| id | sev | o que é | por que continua aberto |
|---|---|---|---|
| A-12 | médio | uma chave ativa por vez: o JWKS publica um par só, então uma rotação invalida de imediato os tokens em circulação | o `kid` já existe e é o que torna a convivência possível; suportar um conjunto de chaves não estava nos critérios da #34 |
| A-13 | médio | sem `aud` no token: qualquer serviço que confie na chave aceita qualquer token emitido | emitir e validar audience exige ordem de deploy (auth primeiro, product depois), senão todo token em circulação é recusado no intervalo. Merece PR próprio com essa ordem escrita |
| A-18 | médio | o limite por origem só vale contra tráfego desatento enquanto não houver, na borda, um proxy que **escreva** o `X-Forwarded-For` (o nginx faz com `$proxy_add_x_forwarded_for`). Hoje quem manda o cabeçalho escolhe a própria cota | quem protege a senha é a contagem por conta (A-17), que não depende de cabeçalho. O `ClientResolver` já está pronto para o dia em que a borda existir, e passa a valer sem mudar código |
| A-19 | médio | com o limite por origem contornável, cada tentativa de login continua custando uma verificação de BCrypt, que é cara de propósito: volume alto vira consumo de CPU | fechar isso exige limitar na borda, que é onde a conexão termina. Fica registrado em vez de escondido |
| A-14 | baixo | sem refresh token nem revogação: um token vazado vale até expirar, em uma hora | fora do escopo desta entrega, listado no backlog |
| A-15 | médio | token guardado em `sessionStorage` no frontend, alcançável por XSS | a linha anterior deste documento dizia "mitigado pela CSP", e **CSP não existia**: foi escrita como se a proteção estivesse no lugar. Hoje existe (ver `next.config.ts`), e ela limita a saída do dado, não a execução do script, porque o `script-src` ainda precisa de `'unsafe-inline'` para a hidratação do Next. A correção real é cookie `HttpOnly`, que muda o contrato com o frontend |

### Verificado e OK

Para ninguém reinvestigar:

- **401 e 403 não passam pelo `GlobalExceptionHandler`.** O Spring Security responde no filtro, antes do dispatcher. Nenhum `AuthenticationEntryPoint` ou `AccessDeniedHandler` customizado é necessário.
- **`UserDetailsServiceImpl` não tem consumidor explícito** desde a remoção do filtro, e continua necessário: o Spring o descobre como bean e o usa no `DaoAuthenticationProvider` do login. Os testes de login provam.
- **O CORS funciona sem `.cors()` explícito na cadeia**, porque o Spring Security aplica o bean quando ele existe. A chamada está lá por visibilidade, e isso foi confirmado por experimento, não deduzido.
- **O endpoint JWKS não publica material privado.** `toPublicJWK()` descarta `d`, `p`, `q`, `dp`, `dq`, `qi`, e um teste confere a ausência de cada um.
- **Token com `alg: none` é recusado.** Testado contra a stack de pé: `DELETE` de produto com um token forjado sem assinatura responde `401`.
- **Payload adulterado com a assinatura original é recusado.** Trocar o `sub` para outro endereço e manter a terceira parte devolve `401`.
- **O emissor é validado** no product-service, por `JwtValidators.createDefaultWithIssuer`.
- **A documentação viva é desligada por padrão** (`SWAGGER_ENABLED`), e o compose de desenvolvimento a liga explicitamente. Publicá-la entrega o mapa das rotas e de quem precisa de token.

## Testes

| Teste | Risco que protege |
|---|---|
| `TokenAuthenticationTest` (6) | rota protegida deixar de exigir identificação; aceitar assinatura desconhecida, token expirado ou malformado; barrar token válido |
| `NoPiiInLogsTest` (5) | e-mail ou senha no log, incluindo pelo caminho autenticado |
| `ConcurrentRegistrationTest` (1) | duas contas com o mesmo e-mail em registro simultâneo |
| `PasswordPolicyTest` (5) | política de senha afrouxar sem ninguém notar |
| `RateLimitTest` (5) | força bruta em login sair de graça |
| `CorsTest` (4) | API deixar de responder ao frontend, ou responder a qualquer origem |
| `AdminInitializerTest` (3) | conta administrativa criada sem querer, ou com senha padrão |
| `ErrorResponseTest` (6) | erro de cliente virar 500 |
| `AuthorizationMatrixTest` (6) | rota pública fechar, ou rota nova nascer aberta |
| `JwksEndpointTest` (3) | chave privada vazar pelo endpoint público; `kid` do token não bater com o publicado |
| `CorrelationIdTest` (5) | requisição sem rastro; identificador forjado no log; vazamento de contexto entre requisições |
| `HealthEndpointTest` (12) | health inacessível ao orquestrador; Actuator expondo configuração interna |

**80 testes, cobertura medida em 92% de linha e 77% de ramo** (20/08/2026).

### Prova de que os testes não são vacuosos

Cada regra nova foi quebrada de propósito, uma por vez, com a suíte rodando entre cada quebra.

| Alvo | Mutações | Acusadas |
|---|---|---|
| `ClientResolver` | 4 | 4 |
| contagem de erros por conta | 4 | 4 |
| documentação da API desligada por padrão | 2 | 2 |

As que mais importam, porque quebram em silêncio: voltar a agrupar todo mundo no endereço da conexão (3 casos acusam), ler o **primeiro** valor do `X-Forwarded-For`, que é o que o atacante escreve (3 acusam), recusar **antes** de verificar a senha, que é o bug do bloqueio (1 acusa), e tirar o `@PreAuthorize` do `PUT` das configurações (1 acusa).

**A primeira rodada mentiu, e vale registrar como.** Ela devolveu "6 de 6 escaparam", incluindo mutações impossíveis de passar despercebidas. O defeito era do script: ele não encontrava o `mvnw.cmd`, ficava sem saída e caía num relatório verde antigo em `target/surefire-reports`. Foi pego conferindo na mão uma das mutações, que acusava 3 falhas. O script passou a apagar o relatório antes de cada rodada e a tratar a ausência dele como erro. O modo de falha é traiçoeiro porque um verificador quebrado aprova tudo, e parece rigor.

### O que NÃO está coberto

- **Não existe endpoint protegido próprio**, então a matriz não consegue exercitar "autenticado sem permissão recebe 403" neste serviço. Isso está provado no product-service. Quando surgir o primeiro endpoint protegido aqui, ele entra na matriz com as três linhas.
- **Rotação com duas chaves ativas** não tem teste porque o recurso não existe (A-12).
- **A travessia do correlation id entre os dois serviços** é verificada por metade em cada lado; falta um teste de sistema com os dois no ar.
- **Ramos de erro internos** (falha ao ler a chave, banco indisponível no meio da transação) não têm caso próprio. É o que segura o piso de ramo em 55%.

## Como verificar em produção

Somente leitura:

```bash
# O serviço está pronto para receber tráfego?
curl -s http://HOST:8080/actuator/health/readiness

# A chave publicada é a esperada? (compare o kid com o do token emitido)
curl -s http://HOST:8080/.well-known/jwks.json | jq '.keys[] | {kid, alg, use}'

# Nenhum parâmetro privado pode aparecer:
curl -s http://HOST:8080/.well-known/jwks.json | jq '.keys[] | keys' | grep -E '"d"|"p"|"q"' && echo "ALERTA: chave privada exposta"
```

```sql
-- Contas duplicadas por e-mail: precisa devolver zero linhas.
SELECT email, COUNT(*) FROM tb_users GROUP BY email HAVING COUNT(*) > 1;

-- Papéis existentes, semeados pela migration.
SELECT * FROM tb_roles;
```

No log, uma requisição inteira pelo identificador:

```bash
grep '"correlationId":"SEU-ID-AQUI"' /var/log/florescer/auth.log
```

## Dívida conhecida

- `spring-security-test` está declarado no POM e não é usado por nenhum teste. A matriz autentica com token real, que é mais fiel. Remover a dependência é limpeza pendente.
- Não há verificação de e-mail nem recuperação de senha.
- O limite por origem só é confiável com um proxy de borda que escreva o `X-Forwarded-For` (A-18), e cada tentativa ainda custa um BCrypt (A-19).

## Histórico

| Data | O que mudou |
|---|---|
| 11/08/2026 | JWKS com `kid`, health, correlation id, matriz de autorização, corrida no registro |
| 10/08/2026 | Flyway, `BigDecimal`, remoção do `JwtAuthFilter`, política de senha |
| 09/08/2026 | rotação da chave RSA e purga do histórico, CORS, rate limit, PII fora do log |
