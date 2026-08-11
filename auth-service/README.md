# auth-service

Identidade: registro, login e emissão dos tokens que o resto do sistema aceita. Porta 8080, MySQL.

Para visão geral, arquitetura e como subir tudo, ver o [README da raiz](../README.md).

## Endpoints

| Método | Rota | Quem pode | O que faz |
|---|---|---|---|
| `POST` | `/v1/auth/register` | qualquer um | cria conta com papel BASIC |
| `POST` | `/v1/auth/login` | qualquer um | devolve o token de acesso |
| `GET` | `/.well-known/jwks.json` | qualquer um | chave pública que valida os tokens |
| `GET` | `/actuator/health` | qualquer um | UP ou DOWN, sem detalhe |

Todas as rotas são públicas. O serviço ainda não tem endpoint protegido próprio, e a regra padrão (`anyRequest().authenticated()`) existe para que um endpoint novo nasça fechado.

## Decisões que não são óbvias

**As chaves não têm valor padrão.** Sem `RSA_PRIVATE_KEY` e `RSA_PUBLIC_KEY` a aplicação não sobe. Um default faria o serviço assinar tokens com uma chave conhecida por quem lê o repositório.

**O e-mail não vai para o log.** As tentativas de registro e login são registradas com um pseudônimo estável (SHA-256 com salt), que permite contar e correlacionar tentativas sem gravar o endereço. O log costuma ter retenção mais longa e acesso mais amplo que o banco.

**Senha: 12 a 64 caracteres, sem exigir símbolo.** Segue o NIST 800-63B, que coloca comprimento acima de composição. O teto de 64 não é política, é o limite prático do BCrypt (72 bytes).

**O login não valida formato de senha.** Só `@NotBlank`. Validar formato no login devolveria 400 explicando a regra a quem está tentando adivinhar, e travaria quem tem senha antiga quando a política mudar.

**Rate limit por origem** em login e registro, antes da consulta ao banco e da verificação de BCrypt, que é cara de propósito.

## Configuração

| Variável | Obrigatória | O que é |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | sim | acesso ao MySQL |
| `RSA_PRIVATE_KEY` | sim | PEM ou `file:`/`classpath:` |
| `RSA_PUBLIC_KEY` | sim | idem |
| `CORS_ALLOWED_ORIGINS` | não | origens do navegador, separadas por vírgula |
| `LOG_PSEUDONYM_SALT` | não | salt do pseudônimo; sem ele o log ainda funciona, só perde resistência a teste de hipótese |
| `ADMIN_ENABLED` | não | cria a conta administrativa inicial; desligado por padrão |

## Testes

```bash
./mvnw verify
```

Sobe um MySQL real em container. `verify` inclui o piso de cobertura: 80% de linha, 55% de ramo.
