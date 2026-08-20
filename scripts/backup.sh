#!/usr/bin/env bash
#
# Backup do que não pode ser recriado.
#
# São três coisas, e as três precisam sair juntas:
#
#   - o catálogo (PostgreSQL): nomes, preços, descrições, cuidados;
#   - as contas (MySQL): sem elas ninguém entra no painel;
#   - as fotos (volume de uploads): o banco guarda o caminho, não a imagem.
#
# Um backup só do banco deixa o catálogo apontando para fotos que não existem
# mais, e um site de plantas sem foto não vende. Por isso os três vão no mesmo
# arquivo, com a mesma data: restaurar metade é pior que não restaurar.
#
# Uso:
#   ./scripts/backup.sh                      # grava em ./backups
#   DESTINO=/mnt/hd ./scripts/backup.sh      # grava em outro lugar
#
# Para rodar todo dia às 3 da manhã, no servidor:
#   0 3 * * * cd /opt/florescer && ./scripts/backup.sh >> /var/log/florescer-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")/.."

DESTINO="${DESTINO:-./backups}"
# Quantos dias de backup guardar. Trinta dias cobrem o caso mais comum, que é
# alguém perceber tarde que apagou uma planta sem querer.
RETENCAO_DIAS="${RETENCAO_DIAS:-30}"
COMPOSE="${COMPOSE:-docker compose}"

DATA="$(date +%Y-%m-%d_%H%M%S)"
PASTA="${DESTINO}/${DATA}"

# Lê as credenciais do mesmo .env que sobe a stack, para não haver duas fontes
# de verdade sobre a senha do banco.
#
# Uma variável por vez, e nunca `source`: o .env do Compose não é script de
# shell. A chave RSA ocupa várias linhas e tem espaços, e o `source` tenta
# executá-la, falhando com "PRIVATE: command not found".
ler_env() {
  [ -f .env ] || return 0
  grep -m1 "^$1=" .env 2>/dev/null | cut -d= -f2- | tr -d ''
}

DB_USERNAME="${DB_USERNAME:-$(ler_env DB_USERNAME)}"
DB_PASSWORD="${DB_PASSWORD:-$(ler_env DB_PASSWORD)}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-$(ler_env DB_ROOT_PASSWORD)}"

: "${DB_USERNAME:?DB_USERNAME não definido: o .env está no lugar?}"
: "${DB_PASSWORD:?DB_PASSWORD não definido}"
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD não definido}"

echo "==> Backup em ${PASTA}"
mkdir -p "${PASTA}"

# --- catálogo (PostgreSQL) -------------------------------------------------
# --clean --if-exists deixa o dump pronto para restaurar por cima de um banco
# que já tem as tabelas, que é o caso de toda restauração real.
echo "--> catálogo"
$COMPOSE exec -T product-db pg_dump \
  --username "${DB_USERNAME}" \
  --dbname dbproduct \
  --clean --if-exists \
  | gzip > "${PASTA}/catalogo.sql.gz"

# --- contas (MySQL) --------------------------------------------------------
# Como root: o usuário da aplicação não costuma ter permissão de LOCK TABLES,
# e o dump sai inconsistente ou falha.
echo "--> contas"
$COMPOSE exec -T auth-db mysqldump \
  --user=root \
  --password="${DB_ROOT_PASSWORD}" \
  --single-transaction \
  --databases authdb \
  2>/dev/null \
  | gzip > "${PASTA}/contas.sql.gz"

# --- fotos -----------------------------------------------------------------
# Direto do volume, com um container descartável: o container da aplicação roda
# como usuário não-root e pode não conseguir ler tudo.
echo "--> fotos"
# O tar sai pela saída padrão, em vez de gravar num diretório montado do host.
# Montar o host exigiria converter o caminho em cada sistema (no Git Bash o
# `pwd` devolve /d/..., que o Docker no Windows não entende, e o resultado é uma
# pasta de nome estranho e nenhum arquivo). O redirecionamento funciona igual em
# qualquer lugar.
#
# O `cd` vai dentro do `sh -c`, e não como `-C /dados` na linha do docker: o Git
# Bash reescreve argumentos que parecem caminho POSIX, e `-C /dados` chegava ao
# container como `-C C:/Users/.../dados`. O resultado era um .tar.gz de zero
# byte, que passaria por backup até o dia de precisar dele.
docker run --rm \
  -v florescer_product-uploads:/dados:ro \
  alpine:3.20 \
  sh -c 'cd /dados && tar czf - .' > "${PASTA}/fotos.tar.gz"

# --- conferência -----------------------------------------------------------
# Um arquivo vazio ou truncado tem cara de backup até a hora de precisar dele.
# Aqui cada um é aberto para valer, e o script falha se algum não abrir.
echo "==> Conferindo"
gzip -t "${PASTA}/catalogo.sql.gz"
gzip -t "${PASTA}/contas.sql.gz"
gzip -t "${PASTA}/fotos.tar.gz"

# Um dump vazio ainda passa no `gzip -t`: o arquivo abre, e não tem nada dentro.
# O que prova conteúdo é encontrar a tabela.
if ! zcat "${PASTA}/catalogo.sql.gz" | grep -q "CREATE TABLE public.product"; then
  echo "ERRO: o dump do catálogo não contém a tabela product" >&2
  exit 1
fi

if ! zcat "${PASTA}/contas.sql.gz" | grep -qi "CREATE TABLE"; then
  echo "ERRO: o dump das contas não contém tabela nenhuma" >&2
  exit 1
fi

# O pg_dump escreve os dados como um bloco COPY, e não como INSERT: as linhas
# ficam entre o cabeçalho do COPY e um "\." sozinho. Contar INSERT daria zero
# aqui, e um backup vazio passaria por bom.
PLANTAS=$(zcat "${PASTA}/catalogo.sql.gz" \
  | awk '/^COPY public\.product /{dentro=1; next} dentro && /^\\\.$/{dentro=0} dentro{n++} END{print n+0}')

if [ "${PLANTAS}" -eq 0 ]; then
  echo "AVISO: o catálogo foi salvo sem nenhuma planta." >&2
fi

FOTOS=$(tar tzf "${PASTA}/fotos.tar.gz" | grep -c "\." || true)

# --- limpeza ---------------------------------------------------------------
if [ -d "${DESTINO}" ]; then
  find "${DESTINO}" -maxdepth 1 -type d -name '20*' -mtime "+${RETENCAO_DIAS}" -exec rm -rf {} + 2>/dev/null || true
fi

TAMANHO=$(du -sh "${PASTA}" | cut -f1)
echo "==> Pronto: ${PASTA} (${TAMANHO})"
echo "    plantas no dump: ${PLANTAS}"
echo "    fotos:           ${FOTOS}"
echo
echo "    Backup que nunca foi restaurado é hipótese. Para testar:"
echo "      ./scripts/restore.sh ${PASTA}"
