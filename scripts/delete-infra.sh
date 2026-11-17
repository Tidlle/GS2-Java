#!/usr/bin/env bash
# =====================================================================================
# Zela · Remove TODOS os recursos da Azure criados por scripts/infra-azure.sh
# =====================================================================================
# Apaga o Resource Group inteiro (Web App, App Service Plan e Azure SQL).
# Use só depois da correção da GS (API e banco devem ficar no ar até ~30/11/2026).
#
# Uso: bash scripts/delete-infra.sh
set -euo pipefail

RG="${RG:-rg-zela}"

if ! az group show -n "$RG" >/dev/null 2>&1; then
  echo "Resource Group ${RG} não existe. Nada a fazer."; exit 0
fi

echo "Recursos que serão APAGADOS (não há como desfazer):"
az resource list -g "$RG" --query "[].{nome:name, tipo:type}" -o table
echo
read -r -p "Digite o nome do Resource Group (${RG}) para confirmar: " confirmacao
[ "$confirmacao" = "$RG" ] || { echo "Cancelado."; exit 1; }

az group delete -n "$RG" --yes --no-wait
echo "Exclusão solicitada. Acompanhe no portal (leva alguns minutos):"
echo "  az group show -n ${RG} --query properties.provisioningState -o tsv"
