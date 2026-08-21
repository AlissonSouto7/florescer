# Backup e verificação de deploy

O que protege o catálogo de sumir, e o que impede um deploy quebrado de passar por bem-sucedido.

**Onde fica**: `scripts/backup.sh`, `scripts/restore.sh`, `scripts/smoke.sh`.
**Status**: funcional, testado com restauração real.
**Última revisão**: 20/08/2026.

## Por que existe

Perder o catálogo é o único problema deste projeto que **não tem conserto**. Um buraco de segurança pode ser fechado, um bug pode ser corrigido, mas uma planta que a vendedora cadastrou (foto, descrição, preço, cuidados) não volta se o volume do banco se perder.

O segundo motivo é mais sutil: um deploy termina dizendo "sucesso" quando o container sobe, e não quando o site funciona. A aplicação pode iniciar e falhar ao falar com o banco, a vitrine pode responder `200` sem nenhuma planta, e a foto pode dar `404` porque o volume não montou. Sem verificação, quem descobre é o cliente.

## O que entra no backup

Três coisas, e as três precisam sair juntas:

| | Onde vive | Por que sozinho não serve |
|---|---|---|
| catálogo | PostgreSQL | sem as fotos, aponta para arquivos que não existem |
| contas | MySQL | sem elas ninguém entra no painel |
| fotos | volume `product-uploads` | o banco guarda o **caminho**, nunca a imagem |

Restaurar metade é pior que não restaurar: um catálogo cheio de plantas sem foto não vende, e o problema parece do site, não do backup.

## Como usar

```bash
./scripts/backup.sh                    # grava em ./backups/AAAA-MM-DD_HHMMSS
DESTINO=/mnt/hd ./scripts/backup.sh    # em outro lugar
RETENCAO_DIAS=90 ./scripts/backup.sh   # guardar por mais tempo

./scripts/restore.sh --conferir backups/2026-08-20_030000   # só olha, não altera
./scripts/restore.sh backups/2026-08-20_030000              # restaura de verdade

SITE=https://florescer.com.br ./scripts/smoke.sh            # verifica o site no ar
```

No servidor, todo dia às três da manhã:

```
0 3 * * * cd /opt/florescer && ./scripts/backup.sh >> /var/log/florescer-backup.log 2>&1
```

## Regras e por quê

**O backup confere o que gerou.** `gzip -t` em cada arquivo, e mais: procura a tabela dentro do dump. Um arquivo vazio passa no teste de integridade sem problema nenhum, porque ele abre; o que não abre é o dia de precisar dele.

**A restauração exige digitar `RESTAURAR`.** Ela sobrescreve o que está no ar. Um "s/n" é fácil de responder no automático.

**As aplicações param durante a restauração.** Restaurar com elas escrevendo produz um estado que não é nem o antigo nem o do backup.

**O `--conferir` existe para ser usado sem medo.** Backup que nunca foi restaurado é hipótese, e testar não pode exigir coragem.

**O smoke test sai com código 1 quando algo falha**, para o deploy poder parar em cima disso, em vez de seguir e declarar sucesso.

**O smoke test não confere só o status.** Verifica se o catálogo tem planta, se o HTML da vitrine sai do servidor com as plantas dentro, se a foto responde com `content-type` de imagem, e se cadastrar sem token continua sendo recusado.

## Verificado, e não deduzido

**A restauração foi testada apagando dado de verdade.** Em 20/08/2026:

```
ANTES do desastre:      7 plantas
  apagada: Cacto Mandacaru (c2277a0f-...)   DELETE -> 204
DEPOIS do desastre:     6 plantas
DEPOIS da restauração:  7 plantas
  a planta apagada voltou: Cacto Mandacaru
  todos os campos batem com o original: True
  a foto dela responde: 200, 76487 bytes, PNG=True
```

A foto importa tanto quanto a linha no banco: restaurar só o dump devolveria a planta apontando para um arquivo inexistente.

**O smoke test foi provado não-vacuoso.** Com `product-service` parado:

```
  FALHA catálogo responde            (esperava 200, veio 500)
  FALHA catálogo está vazio          (totalElements=ausente)
  FALHA nenhuma foto para conferir
  FALHA cadastro sem token NÃO foi recusado  (veio 500)
  ==> 4 verificação(ões) falharam.       exit=1
```

Com tudo de pé, as 10 verificações passam e o `exit` é 0.

## O backup foi parar no repositório, e isso é o mais importante daqui

Em 21/08/2026, durante uma revisão, apareceu uma pasta `backups/` **versionada**, com três arquivos e 384 KB. Um deles é o dump do banco de contas.

O que estava público:

| | |
|---|---|
| tabelas com dado | `tb_users`, `tb_roles`, `tb_users_roles`, `flyway_schema_history` |
| endereços | 1, num domínio `.test` |
| hashes BCrypt | 1, de uma conta com papel **ADMIN** |

**Como entrou.** O `.gitignore` tem `/backups/`, mas no commit `8c57111` essa linha **ainda não existia**, e um `git add -A` levou a pasta junto. A regra chegou depois, e gitignore não desrastreia o que já está rastreado: ela passou a valer para arquivo novo e não mexeu no que já tinha entrado.

**O que isso significa de verdade.** A conta não é a da vendedora (`.test` é domínio reservado para teste, e a real está noutro domínio), mas **ela existe no banco e tem papel ADMIN**. Hash público é hash sem limite de tentativas: quem baixa o repositório quebra offline, sem passar por rate limit nenhum, e a política de senha do projeto é o que decide se isso é viável ou não.

**O que foi feito**: a pasta saiu do rastreamento e o `.gitignore` ganhou o registro de como ela entrou.

**O que continua pendente, e depende de decisão**: o arquivo continua no **histórico** do git, então quem clonar ainda alcança. Tirar de lá exige reescrever o histórico e um push forçado, que não é ação para tomar sozinho. E a conta `.test` deveria ser apagada do banco de qualquer forma, porque ela não deveria existir em ambiente nenhum.

**A lição operacional**, que vale mais que a correção: `git add -A` num diretório onde o script de backup acabou de rodar leva o backup junto. Um `git status` antes de commitar mostra isso em uma linha.

## Armadilhas encontradas ao escrever

**O `.env` não é script de shell.** `source .env` falha com `PRIVATE: command not found`, porque a chave RSA ocupa várias linhas e tem espaços. Os scripts leem variável por variável com `grep`.

**O nome da tabela é `product`, e o `pg_dump` grava com `COPY`, não `INSERT`.** A primeira versão procurava `tb_products` e contava linhas `INSERT`: teria reportado zero plantas num backup correto, ou pior, aprovado um backup vazio.

**O Git Bash reescreve caminhos que parecem POSIX.** `-C /dados` na linha do `docker run` chegava ao container como `-C C:/Users/.../dados`, e o resultado era um `fotos.tar.gz` de **zero byte**, que passa por backup até a hora de precisar. O `cd` foi para dentro do `sh -c`, onde o Git Bash não mexe.

Nas três, o defeito produzia um arquivo com cara de backup. É por isso que a conferência olha dentro, e não só se o arquivo existe.

## O que NÃO está coberto

- **O backup fica na mesma máquina.** Se o servidor se perder inteiro, os backups vão junto. Copiar para fora (outra máquina, armazenamento externo) é o próximo passo, e depende de onde o site for hospedado.
- **Nada avisa quando o backup falha.** Rodando por cron, uma falha vai para o log e ninguém lê log. Precisa de um aviso ativo.
- **A restauração não foi testada em máquina limpa**, só por cima de uma stack existente. Restaurar num servidor recém-instalado é um caminho diferente.
- **O smoke test não faz login nem cadastra.** Verifica o que dá para verificar sem credencial; o fluxo autenticado continua sendo verificação manual.

## Como verificar em produção

```bash
# O último backup é de quando, e tem tamanho plausível?
ls -lht backups/ | head -3
du -sh backups/* | tail -3

# O último backup presta? (não altera nada)
./scripts/restore.sh --conferir "$(ls -dt backups/*/ | head -1)"

# O site está de pé de verdade?
SITE=https://florescer.com.br ./scripts/smoke.sh
```

## Dívida conhecida

- Cópia do backup para fora do servidor.
- Aviso ativo quando o backup falha.
- Smoke test cobrindo o fluxo autenticado.
- O deploy ainda não chama o smoke test: o passo no `cd.yml` continua sendo um placeholder até existir destino.

## Histórico

| Data | O que mudou |
|---|---|
| 20/08/2026 | backup, restauração e smoke test, com restauração testada de verdade |
