#!/usr/bin/env bash
#
# Restaura um backup gerado por backup.sh.
#
# Existe por um motivo simples: backup que nunca foi restaurado é hipótese, não
# garantia. O dia de descobrir que o dump estava vazio não pode ser o dia em que
# o catálogo se perdeu.
#
# Uso:
#   ./scripts/restore.sh backups/2026-08-20_030000
#
# ATENÇÃO: isto SOBRESCREVE os dados atuais. Para só conferir que o backup
# presta, sem tocar em nada, use:
#   ./scripts/restore.sh --conferir backups/2026-08-20_030000
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE="${COMPOSE:-docker compose}"
SO_CONFERIR=false

if [ "${1:-}" = "--conferir" ]; then
  SO_CONFERIR=true
  shift
fi

PASTA="${1:-}"
if [ -z "${PASTA}" ] || [ ! -d "${PASTA}" ]; then
  echo "Uso: $0 [--conferir] CAMINHO_DO_BACKUP" >&2
  echo "Backups disponíveis:" >&2
  ls -1 backups/ 2>/dev/null | sed 's/^/  /' >&2 || echo "  (nenhum)" >&2
  exit 1
fi

for arquivo in catalogo.sql.gz contas.sql.gz fotos.tar.gz; do
  [ -f "${PASTA}/${arquivo}" ] || { echo "ERRO: falta ${arquivo} em ${PASTA}" >&2; exit 1; }
done

# --- o que tem dentro ------------------------------------------------------
echo "==> Conteúdo de ${PASTA}"
gzip -t "${PASTA}/catalogo.sql.gz" && echo "    catálogo: arquivo íntegro"
gzip -t "${PASTA}/contas.sql.gz"   && echo "    contas:   arquivo íntegro"
gzip -t "${PASTA}/fotos.tar.gz"    && echo "    fotos:    arquivo íntegro"

# O pg_dump grava os dados num bloco COPY, e nao como INSERT.
# O pg_dump grava os dados num bloco COPY, e não como INSERT: contar INSERT
# daria zero, e um backup vazio passaria por bom.
PLANTAS=$(zcat "${PASTA}/catalogo.sql.gz" \
  | awk '/^COPY public\.product /{dentro=1; next} dentro && /^\\\.$/{dentro=0} dentro{n++} END{print n+0}')
FOTOS=$(tar tzf "${PASTA}/fotos.tar.gz" | grep -c "\." || true)
echo "    plantas: ${PLANTAS} | fotos: ${FOTOS}"

if [ "${SO_CONFERIR}" = true ]; then
  echo "==> Só conferência. Nada foi alterado."
  exit 0
fi

# --- confirmação -----------------------------------------------------------
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

: "${DB_USERNAME:?DB_USERNAME não definido}"
: "${DB_ROOT_PASSWORD:?DB_ROOT_PASSWORD não definido}"

echo
echo "Isto SOBRESCREVE o catálogo, as contas e as fotos que estão no ar."
read -r -p "Digite RESTAURAR para continuar: " resposta
[ "${resposta}" = "RESTAURAR" ] || { echo "Cancelado."; exit 1; }

# --- restauração -----------------------------------------------------------
# As aplicações param antes: restaurar com elas escrevendo produz um estado que
# não é nem o antigo nem o do backup.
echo "==> Parando as aplicações"
$COMPOSE stop auth-service product-service frontend

echo "==> Catálogo"
zcat "${PASTA}/catalogo.sql.gz" | $COMPOSE exec -T product-db \
  psql --username "${DB_USERNAME}" --dbname dbproduct --quiet

echo "==> Contas"
zcat "${PASTA}/contas.sql.gz" | $COMPOSE exec -T auth-db \
  mysql --user=root --password="${DB_ROOT_PASSWORD}" 2>/dev/null

echo "==> Fotos"
# Pela entrada padrão, pelo mesmo motivo do backup: nada de montar caminho do
# host, que muda de formato entre sistemas.
docker run --rm -i \
  -v florescer_product-uploads:/dados \
  alpine:3.20 \
  sh -c 'rm -rf /dados/* && cd /dados && tar xzf -' < "${PASTA}/fotos.tar.gz"

echo "==> Subindo de volta"
$COMPOSE start auth-service product-service frontend

echo
echo "==> Restaurado. Confira antes de considerar resolvido:"
echo "    ./scripts/smoke.sh"
