#!/usr/bin/env bash
#
# Confere que o site está de pé de verdade, e não só que o container subiu.
#
# A diferença importa: a aplicação pode iniciar e falhar ao falar com o banco, a
# vitrine pode responder 200 sem nenhuma planta, e a foto pode dar 404 porque o
# volume não montou. Um deploy que termina dizendo "sucesso" nesses casos entrega
# um site quebrado, e quem descobre é o cliente.
#
# Uso:
#   ./scripts/smoke.sh                          # contra http://localhost:3000
#   SITE=https://florescer.com.br ./scripts/smoke.sh
#
# Sai com código 1 se qualquer verificação falhar, para o deploy poder parar.
set -uo pipefail

SITE="${SITE:-http://localhost:3000}"
FALHAS=0

verde()   { printf '  \033[32mOK   \033[0m %s\n' "$1"; }
vermelho(){ printf '  \033[31mFALHA\033[0m %s%s\n' "$1" "${2:+  ($2)}"; FALHAS=$((FALHAS + 1)); }

checa() {
  local rotulo="$1" esperado="$2" caminho="$3"
  local codigo
  codigo=$(curl -s -o /dev/null -m 15 -w '%{http_code}' "${SITE}${caminho}" 2>/dev/null)
  if [ "${codigo}" = "${esperado}" ]; then
    verde "${rotulo}"
  else
    vermelho "${rotulo}" "esperava ${esperado}, veio ${codigo}"
  fi
}

echo "==> Verificando ${SITE}"

# --- as páginas respondem --------------------------------------------------
checa "vitrine responde"            200 "/"
checa "login responde"              200 "/login"
checa "catálogo responde"           200 "/api/product?size=1"
checa "sitemap responde"            200 "/sitemap.xml"
checa "robots responde"             200 "/robots.txt"
checa "endereço inexistente dá 404" 404 "/isso-nao-existe"

# --- a vitrine tem conteúdo ------------------------------------------------
# Responder 200 com a lista vazia parece "nenhuma planta cadastrada", e é o
# sintoma de banco vazio ou de restauração pela metade.
PLANTAS=$(curl -s -m 15 "${SITE}/api/product?size=1" 2>/dev/null \
  | grep -oE '"totalElements":[0-9]+' | grep -oE '[0-9]+$')

if [ -n "${PLANTAS:-}" ] && [ "${PLANTAS}" -gt 0 ]; then
  verde "catálogo tem ${PLANTAS} planta(s)"
else
  vermelho "catálogo está vazio" "totalElements=${PLANTAS:-ausente}"
fi

# O HTML precisa sair do servidor já com as plantas: é disso que depende o
# Google achar a loja. Vazio aqui com catálogo cheio significa que a renderização
# no servidor não está alcançando a API.
NA_VITRINE=$(curl -s -m 15 "${SITE}/" 2>/dev/null | grep -oc 'planta/' || true)
if [ "${NA_VITRINE:-0}" -gt 0 ]; then
  verde "vitrine renderizada no servidor com plantas"
else
  vermelho "o HTML da vitrine não traz nenhuma planta"
fi

# --- a foto chega ----------------------------------------------------------
# O caminho vem do próprio catálogo: uma foto qualquer, a que existir.
FOTO=$(curl -s -m 15 "${SITE}/api/product?size=1" 2>/dev/null \
  | grep -oE '"imageUrl":"[^"]+"' | head -1 | sed 's/.*"imageUrl":"//;s/"$//' | sed 's|^https\?://[^/]*||')

if [ -n "${FOTO}" ]; then
  TIPO=$(curl -s -o /dev/null -m 15 -w '%{content_type}' "${SITE}${FOTO}" 2>/dev/null)
  TAMANHO=$(curl -s -o /dev/null -m 15 -w '%{size_download}' "${SITE}${FOTO}" 2>/dev/null)

  case "${TIPO}" in
    image/*) verde "foto servida (${TIPO}, ${TAMANHO} bytes)" ;;
    *)       vermelho "a foto não é imagem" "content-type ${TIPO:-vazio}" ;;
  esac
else
  vermelho "nenhuma foto para conferir" "o catálogo não devolveu imageUrl"
fi

# --- o que deve continuar fechado ------------------------------------------
# Cadastrar sem token precisa ser recusado. Se isto virar 201, qualquer pessoa
# na internet escreve no catálogo.
CODIGO=$(curl -s -o /dev/null -m 15 -w '%{http_code}' -X POST "${SITE}/api/product" 2>/dev/null)
case "${CODIGO}" in
  401|403) verde "cadastro sem token é recusado (${CODIGO})" ;;
  *)       vermelho "cadastro sem token NÃO foi recusado" "veio ${CODIGO}" ;;
esac

echo
if [ "${FALHAS}" -eq 0 ]; then
  echo "==> Tudo certo."
  exit 0
fi

echo "==> ${FALHAS} verificação(ões) falharam."
exit 1
