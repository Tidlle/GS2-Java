#!/usr/bin/env bash
# =====================================================================================
# Zela · Políticas da branch main no Azure Repos (documentadas como código)
# =====================================================================================
# Aplica na main do repositório Zela (todas bloqueantes):
#   1. Mínimo de 1 revisor, e o autor pode aprovar o próprio PR (simulação permitida na GS)
#   2. Work item vinculado obrigatório (todo PR liga a uma tarefa do Azure Boards)
#   3. Revisor padrão obrigatório: o RM do responsável, incluído automaticamente em todo PR
#   4. Comentários do PR resolvidos antes do merge
# Não há build validation de propósito: a CI roda só depois do merge na main.
#
# Uso (depois de "az login"): bash scripts/azure-devops-policies.sh
# Requer a extensão: az extension add --name azure-devops
set -euo pipefail

ORG="${ORG:-https://dev.azure.com/RM5622590186}"
PROJETO="${PROJETO:-Zela}"
REPO="${REPO:-Zela}"
BRANCH="${BRANCH:-main}"
REVISOR="${REVISOR:-RM562259@fiap.com.br}"

REPO_ID=$(az repos show --org "$ORG" -p "$PROJETO" -r "$REPO" --query id -o tsv)
comum=(--org "$ORG" -p "$PROJETO" --repository-id "$REPO_ID" --branch "$BRANCH" --enabled true --blocking true -o none)

# Tipos de política que já existem na branch (para não duplicar ao rodar de novo).
existentes=$(az repos policy list --org "$ORG" -p "$PROJETO" --repository-id "$REPO_ID" --branch "$BRANCH" \
  --query "[].type.displayName" -o tsv)
tem() { grep -qx "$1" <<<"$existentes"; }

tem "Minimum number of reviewers" || az repos policy approver-count create "${comum[@]}" \
  --minimum-approver-count 1 --creator-vote-counts true --allow-downvotes false --reset-on-source-push false
echo "✔ Mínimo de 1 revisor (autor pode aprovar)"

tem "Work item linking" || az repos policy work-item-linking create "${comum[@]}"
echo "✔ Work item vinculado obrigatório"

tem "Required reviewers" || az repos policy required-reviewer create "${comum[@]}" \
  --required-reviewer-ids "$REVISOR" --message "Revisor padrão do projeto"
echo "✔ Revisor padrão: ${REVISOR}"

tem "Comment requirements" || az repos policy comment-required create "${comum[@]}"
echo "✔ Comentários resolvidos antes do merge"

az repos policy list --org "$ORG" -p "$PROJETO" --repository-id "$REPO_ID" --branch "$BRANCH" \
  --query "[].{politica:type.displayName, bloqueante:isBlocking, ativa:isEnabled}" -o table
