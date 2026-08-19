#!/usr/bin/env bash
#
# Configura o lado do GitHub que não vive no repositório: labels, milestones,
# issues do roadmap, environments e branch protection.
#
# Pré-requisitos:
#   gh auth login          (com escopos repo, workflow, read:project, project)
#
# Uso:
#   ./setup-github.sh              labels, milestones, issues e environments
#   ./setup-github.sh protection   branch protection (rodar só DEPOIS do primeiro
#                                  CI, para que os status checks já existam com
#                                  os nomes exatos que o workflow gera)
#
# É idempotente: rodar de novo não duplica nada.
#
set -euo pipefail

REPO="${REPO:-AlissonSouto7/florescer}"
OWNER="${REPO%%/*}"
STEP="${1:-setup}"

echo "==> Repositório: $REPO"
gh repo view "$REPO" --json nameWithOwner -q .nameWithOwner >/dev/null

# ------------------------------------------------------ branch protection ----
# Etapa separada de propósito: exigir um status check que ainda não existe
# deixa o pull request travado sem explicação. Rode depois do primeiro CI.
if [ "$STEP" = "protection" ]; then
  protect() {
    local branch="$1"
    gh api -X PUT "repos/$REPO/branches/$branch/protection" --input - >/dev/null <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "build (auth-service)",
      "build (product-service)",
      "build (florescer-web)",
      "secret scan",
      "analyze (java-kotlin)",
      "analyze (javascript-typescript)"
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false
  },
  "restrictions": null,
  "required_linear_history": true,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_conversation_resolution": true
}
JSON
    echo "    $branch protegida"
  }
  echo "==> Branch protection"
  protect main
  protect develop
  exit 0
fi

# ---------------------------------------------------------------- labels ----
echo "==> Labels"
create_label() {
  gh label create "$1" --repo "$REPO" --color "$2" --description "$3" --force >/dev/null
  echo "    $1"
}
create_label security   "b60205" "Falha ou endurecimento de segurança"
create_label bug        "d73a4a" "Comportamento errado"
create_label feature    "0e8a16" "Funcionalidade nova"
create_label refactor   "5319e7" "Mudança interna sem alterar comportamento"
create_label test       "fbca04" "Cobertura de teste"
create_label docs       "0075ca" "Documentação"
create_label infra      "1d76db" "Build, CI/CD, Docker, ambiente"
create_label task       "c5def5" "Tarefa genérica"

# ------------------------------------------------------------ milestones ----
echo "==> Milestones"
create_milestone() {
  if gh api "repos/$REPO/milestones?state=all" --jq '.[].title' | grep -qxF "$1"; then
    echo "    $1 (já existe)"
  else
    gh api "repos/$REPO/milestones" -f title="$1" -f description="$2" >/dev/null
    echo "    $1"
  fi
}
create_milestone "F2 - Segurança crítica"      "Rotação de chave, upload endurecido, validação viva, CORS, rate limit"
create_milestone "F3 - Funcionar ponta a ponta" "Contrato do frontend, imagens acessíveis, exception handlers"
create_milestone "F4 - Arquitetura"             "Camadas, BigDecimal, transações, Flyway, JWKS"
create_milestone "F5 - Testes"                  "Suíte com Testcontainers, matriz de autorização, coverage gate real"
create_milestone "F6 - Docs e infra"            "README, docker-compose raiz, docs/features, actuator"

milestone_number() {
  gh api "repos/$REPO/milestones?state=all" --jq ".[] | select(.title==\"$1\") | .number"
}

# ---------------------------------------------------------------- issues ----
echo "==> Issues do roadmap"
create_issue() {
  local title="$1" body="$2" labels="$3" milestone="$4"
  if gh issue list --repo "$REPO" --state all --search "\"$title\" in:title" --json title -q '.[].title' | grep -qxF "$title"; then
    echo "    $title (já existe)"
    return
  fi
  gh issue create --repo "$REPO" \
    --title "$title" --body "$body" \
    --label "$labels" --milestone "$milestone" >/dev/null
  echo "    $title"
}

M2="F2 - Segurança crítica"
create_issue "Rotacionar par RSA e carregar chaves por variável de ambiente" \
"A chave privada que assinava todos os JWTs esteve versionada no repositório e deve ser considerada comprometida.

**Critérios de aceite**
- [ ] Par novo gerado fora do repositório
- [ ] auth-service e product-service leem a chave de env var (o auth já suporta desde a fundação do CI)
- [ ] Pasta \`chaves/\` removida dos dois serviços
- [ ] Token assinado com a chave antiga é rejeitado (teste)" \
"security" "$M2"

create_issue "Remover credenciais de admin hardcoded do AdminInitializer" \
"\`AdminInitializer\` cria admin@florescer.com com senha admin123 em todo boot, em qualquer ambiente, e imprime a senha no stdout.

**Critérios de aceite**
- [ ] Credenciais vêm de env, sem default fraco
- [ ] Só roda no profile de desenvolvimento
- [ ] Usa logger, e a senha nunca aparece em log" \
"security" "$M2"

create_issue "Endurecer upload de imagem contra path traversal e XSS armazenado" \
"\`ImageStorageService\` usa \`getOriginalFilename()\` sem sanitização e valida só o Content-Type declarado pelo cliente.

**Critérios de aceite**
- [ ] Nome do arquivo é UUID + extensão derivada do conteúdo real, o nome original é descartado
- [ ] Magic bytes verificados (JPEG, PNG, WebP)
- [ ] Caminho resolvido e validado contra a pasta de uploads
- [ ] Teste com filename \`../../x\` provando que não escapa
- [ ] Teste com .html renomeado para .jpg provando recusa" \
"security" "$M2"

create_issue "Fazer a Bean Validation do product-service realmente executar" \
"O controller recebe \`@Valid String requestJson\` e desserializa com ObjectMapper à mão, então nenhuma constraint dos DTOs roda.

**Critérios de aceite**
- [ ] \`@RequestPart(\"product\") @Valid ProductCreateRequest\` tipado
- [ ] \`@Positive\`/\`@PositiveOrZero\` em preço e estoque
- [ ] Preço negativo devolve 400, com teste
- [ ] \`handleValidationException\` deixa de ser código morto" \
"security" "$M2"

create_issue "Configurar CORS nos dois serviços" \
"Nenhum dos serviços tem configuração de CORS, então o frontend é bloqueado pelo browser antes de qualquer requisição.

**Critérios de aceite**
- [ ] Origem permitida configurável por propriedade, nunca \`*\` com credenciais
- [ ] Preflight OPTIONS responde corretamente
- [ ] Teste cobrindo origem permitida e origem recusada" \
"security" "$M2"

create_issue "Adicionar rate limit em login e registro" \
"Não há limite de tentativas: brute force é livre.

**Critérios de aceite**
- [ ] Limite por IP nos endpoints de autenticação
- [ ] Resposta 429 ao estourar
- [ ] Teste provando o bloqueio" \
"security" "$M2"

M3="F3 - Funcionar ponta a ponta"
create_issue "Corrigir o contrato entre frontend e API" \
"O frontend chama produtos na porta do auth-service, lê \`data.token\` quando a API devolve \`accessToken\`, e lê \`imagePath\` quando o DTO expõe \`imageUrl\`.

**Critérios de aceite**
- [ ] URLs base em constantes, apontando para as portas certas
- [ ] Campos do contrato corrigidos
- [ ] \`innerHTML\` interpolado substituído por createElement/textContent
- [ ] Inputs com name, id, label e autocomplete
- [ ] Fluxo verificado no navegador" \
"bug" "$M3"

create_issue "Tornar as imagens de produto acessíveis" \
"A URL gerada aponta para \`/images/\`, o handler estático está em \`/uploads/**\`, e nenhum dos dois está liberado para acesso anônimo.

**Critérios de aceite**
- [ ] Prefixo unificado
- [ ] \`GET /uploads/**\` liberado
- [ ] Listagem e detalhe devolvem o mesmo formato de URL
- [ ] Imagem carrega para visitante anônimo, verificado no navegador" \
"bug" "$M3"

create_issue "Corrigir os exception handlers dos dois serviços" \
"O handler de \`Exception.class\` intercepta antes do Spring e transforma 400/404/405/413 em 500. Cliente anônimo recebe 403 em vez de 401.

**Critérios de aceite**
- [ ] Handlers específicos para as exceções do dispatcher
- [ ] AccessDenied/Authentication fora do catch-all
- [ ] Anônimo recebe 401, autenticado sem permissão recebe 403
- [ ] \`details\` tipado em vez de Object
- [ ] Testes cobrindo cada código de status" \
"bug" "$M3"

create_issue "Tornar a imagem opcional no PATCH de produto" \
"\`@RequestPart(\"image\")\` é obrigatório por padrão, contrariando a documentação e a lógica do service, que trata imagem nula.

**Critérios de aceite**
- [ ] \`required = false\`
- [ ] PATCH só com campos de texto funciona
- [ ] PATCH vazio devolve o erro de patch inválido" \
"bug" "$M3"

create_issue "Limitar tamanho de página e campos de ordenação" \
"\`size\` sem teto e \`sort\` sem whitelist em endpoint público permitem DoS trivial e 500 com campo inválido.

**Critérios de aceite**
- [ ] Teto de itens por página
- [ ] Whitelist de campos ordenáveis
- [ ] Campo inválido devolve 400, não 500" \
"security" "$M3"

echo
echo "==> Environments"
for env in development staging production; do
  gh api -X PUT "repos/$REPO/environments/$env" >/dev/null
  echo "    $env"
done

echo "    Aplicando required reviewer em production..."
USER_ID=$(gh api users/"$OWNER" --jq .id)
gh api -X PUT "repos/$REPO/environments/production" \
  --input - >/dev/null <<JSON
{
  "wait_timer": 0,
  "reviewers": [{"type": "User", "id": $USER_ID}],
  "deployment_branch_policy": {"protected_branches": false, "custom_branch_policies": true}
}
JSON

# "protected_branches" recusa deploy disparado por TAG, porque tag nao e uma
# branch protegida. Como o CD publica em producao a partir da tag (v0.1.0), a
# regra bloqueava exatamente o unico gatilho que ela precisava permitir: o job
# falhava em 15 segundos, antes de executar qualquer passo e sem mensagem no log.
#
# Com politica customizada, os padroes abaixo dizem o que pode publicar.
for POLICY in '{"name":"v*","type":"tag"}' '{"name":"main","type":"branch"}'; do
  gh api -X POST "repos/$OWNER/$REPO/environments/production/deployment-branch-policies"     --input - >/dev/null <<<"$POLICY" || true
done
echo "    production exige aprovação manual, e aceita a tag v* e a main"

echo
echo "Pronto. Depois do primeiro CI verde, ligue as travas de branch:"
echo "  ./scripts/setup-github.sh protection"
echo
echo "E crie o board manualmente:"
echo "  https://github.com/users/$OWNER/projects/new  (template Board)"
echo "  Colunas: Backlog / Ready / In Progress (WIP 1) / In Review (WIP 3) / Done"
echo "  Depois: Settings do projeto -> Manage access -> adicionar o repositório $REPO"
