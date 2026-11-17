#!/usr/bin/env bash
# =====================================================================================
# Zela · Infraestrutura na Azure via Azure CLI (GS 2026/2 · DevOps Tools & Cloud Computing)
# =====================================================================================
# Cria (ou confere, se já existir) todos os recursos usados pelo Build e pela Release:
#
#   1. Resource Group ............ rg-zela
#   2. Azure SQL Server .......... sql-zela-<rm>     (PaaS, TLS 1.2, firewall)
#   3. Azure SQL Database ........ zela              (tier Basic, collation UTF-8)
#   4. App Service Plan .......... plan-zela         (Linux, B1)
#   5. Web App ................... app-zela-<rm>     (Java 21, Always On, HTTPS, health check)
#
# Mensageria: na nuvem a API roda com MENSAGERIA_MODO=local (eventos processados no próprio
# processo, sem broker). O RabbitMQ é usado localmente com o docker-compose.yml.
#
# Senhas NUNCA ficam neste arquivo: vêm de variável de ambiente ou são digitadas na hora.
# As variáveis sensíveis da API (DB_*, JWT_SECRET, GEMINI_API_KEY) são aplicadas no Web App
# pela pipeline de Release, a partir do variable group "zela-secrets".
#
# Uso (Git Bash ou Azure Cloud Shell, depois de "az login"):
#   bash scripts/infra-azure.sh
# Opcional (sem perguntas):
#   SQL_ADMIN_PASSWORD='...' bash scripts/infra-azure.sh
#
# Região: cada assinatura Azure for Students tem uma lista de regiões permitidas (policy
# "Allowed resource deployment regions"). Padrão: brazilsouth. Se a sua não permitir, ou faltar
# cota, rode de novo com LOCATION=centralus (ou outra da lista: az policy assignment list).
# =====================================================================================
set -euo pipefail
# No Git Bash (Windows), impede que caminhos como /home/zela/uploads virem C:/Program Files/Git/...
export MSYS_NO_PATHCONV=1

# ---------- Parâmetros (podem ser sobrescritos por variáveis de ambiente) ----------
RM="${RM:-rm562259}"                       # deixa os nomes globais únicos
LOCATION="${LOCATION:-brazilsouth}"
RG="${RG:-rg-zela}"
TAGS=(projeto=zela disciplina=devops gs=2026-2)

SQL_SERVER="${SQL_SERVER:-sql-zela-${RM}}"
SQL_DB="${SQL_DB:-zela}"
SQL_ADMIN="${SQL_ADMIN:-zelaadmin}"
SQL_COLLATION="Latin1_General_100_CI_AS_SC_UTF8"   # UTF-8: acentos em colunas VARCHAR

PLAN="${PLAN:-plan-zela}"
PLAN_SKU="${PLAN_SKU:-B1}"
APP="${APP:-app-zela-${RM}}"
RUNTIME="JAVA:21-java21"

# ---------- Funções auxiliares ----------
passo() { printf '\n\033[1;36m>> %s\033[0m\n' "$*"; }
ok()    { printf '   \033[32m✔ %s\033[0m\n' "$*"; }
aviso() { printf '   \033[33m! %s\033[0m\n' "$*"; }
existe() { "$@" >/dev/null 2>&1; }

pedir_senha() { # nome_da_variavel descrição
  local nome="$1" descricao="$2" valor="${!1:-}"
  if [ -z "$valor" ]; then
    read -r -s -p "   ${descricao}: " valor; echo
  fi
  # Mínimo do Azure SQL: 8+ caracteres com maiúscula, minúscula e número.
  if [ ${#valor} -lt 12 ] || ! [[ "$valor" =~ [A-Z] && "$valor" =~ [a-z] && "$valor" =~ [0-9] ]]; then
    echo "   Senha fraca: use 12+ caracteres com maiúscula, minúscula e número." >&2; exit 1
  fi
  case "$valor" in
    *\"*|*\'*|*\$*|*\`*|*\\*|*" "*)
      echo "   Evite aspas, \$, crase, barra invertida e espaço na senha." >&2; exit 1 ;;
  esac
  printf -v "$nome" '%s' "$valor"
}

# ---------- 0. Conferências ----------
passo "Conta e assinatura"
az account show --query "{assinatura:name, estado:state, usuario:user.name}" -o table
if [ "$(az account show --query state -o tsv)" != "Enabled" ]; then
  echo "   A assinatura selecionada não está ativa. Escolha outra: az account set -s <id>" >&2; exit 1
fi
pedir_senha SQL_ADMIN_PASSWORD "Senha do administrador do Azure SQL (${SQL_ADMIN})"

for ns in Microsoft.Web Microsoft.Sql; do
  estado=$(az provider show -n "$ns" --query registrationState -o tsv)
  [ "$estado" = "Registered" ] || { az provider register -n "$ns" --wait; }
done
ok "Providers registrados (Web, Sql)"

# ---------- 1. Resource Group ----------
passo "1/5 Resource Group ${RG} (${LOCATION})"
az group create -n "$RG" -l "$LOCATION" --tags "${TAGS[@]}" -o none
ok "Resource Group pronto"

# ---------- 2. Azure SQL Server + firewall ----------
passo "2/5 Azure SQL Server ${SQL_SERVER}"
if existe az sql server show -g "$RG" -n "$SQL_SERVER"; then
  ok "Já existe (a senha do admin não é alterada)"
else
  az sql server create -g "$RG" -n "$SQL_SERVER" -l "$LOCATION" \
    --admin-user "$SQL_ADMIN" --admin-password "$SQL_ADMIN_PASSWORD" \
    --minimal-tls-version 1.2 --enable-public-network true -o none
  ok "Criado"
fi
# 0.0.0.0 = permite os serviços do Azure (o Web App) acessarem o banco.
az sql server firewall-rule create -g "$RG" -s "$SQL_SERVER" -n PermitirServicosAzure \
  --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o none
ok "Firewall: serviços do Azure"
# IP desta máquina, para consultar o banco (SELECTs do vídeo no SSMS/Azure Data Studio).
MEU_IP="${MEU_IP:-$(curl -s --max-time 10 https://api.ipify.org || true)}"
if [[ "$MEU_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  az sql server firewall-rule create -g "$RG" -s "$SQL_SERVER" -n PermitirMinhaMaquina \
    --start-ip-address "$MEU_IP" --end-ip-address "$MEU_IP" -o none
  ok "Firewall: IP desta máquina (${MEU_IP})"
else
  aviso "IP público não detectado; libere-o depois no portal (Azure SQL > Rede)"
fi

# ---------- 3. Azure SQL Database ----------
passo "3/5 Banco ${SQL_DB} (Basic, ${SQL_COLLATION})"
if existe az sql db show -g "$RG" -s "$SQL_SERVER" -n "$SQL_DB"; then
  collation=$(az sql db show -g "$RG" -s "$SQL_SERVER" -n "$SQL_DB" --query collation -o tsv)
  [ "$collation" = "$SQL_COLLATION" ] && ok "Já existe" \
    || aviso "Já existe com a collation ${collation} (esperado ${SQL_COLLATION}): recrie o banco"
else
  az sql db create -g "$RG" -s "$SQL_SERVER" -n "$SQL_DB" \
    --service-objective Basic --collation "$SQL_COLLATION" \
    --backup-storage-redundancy Local --zone-redundant false -o none
  ok "Criado (as tabelas são criadas pela API com o Flyway no primeiro deploy)"
fi

# ---------- 4. App Service Plan ----------
passo "4/5 App Service Plan ${PLAN} (Linux ${PLAN_SKU})"
if existe az appservice plan show -g "$RG" -n "$PLAN"; then
  ok "Já existe"
else
  az appservice plan create -g "$RG" -n "$PLAN" -l "$LOCATION" --is-linux --sku "$PLAN_SKU" \
    --tags "${TAGS[@]}" -o none
  ok "Criado"
fi

# ---------- 5. Web App (Java 21) ----------
passo "5/5 Web App ${APP} (${RUNTIME})"
if existe az webapp show -g "$RG" -n "$APP"; then
  ok "Já existe"
else
  az webapp create -g "$RG" -p "$PLAN" -n "$APP" --runtime "$RUNTIME" --tags "${TAGS[@]}" -o none
  ok "Criado"
fi
az webapp update -g "$RG" -n "$APP" --https-only true -o none
az webapp config set -g "$RG" -n "$APP" \
  --always-on true --min-tls-version 1.2 --ftps-state Disabled --http20-enabled true \
  --generic-configurations '{"healthCheckPath": "/actuator/health"}' -o none
ok "HTTPS obrigatório, Always On e health check em /actuator/health"
az webapp log config -g "$RG" -n "$APP" --application-logging filesystem \
  --docker-container-logging filesystem --level information -o none
ok "Logs da aplicação ligados (az webapp log tail -g ${RG} -n ${APP})"

# Configurações NÃO sensíveis. A porta (SERVER_PORT=80) é injetada pelo App Service.
# /home é o disco persistente do App Service: as fotos sobrevivem a reinícios e deploys.
az webapp config appsettings set -g "$RG" -n "$APP" -o none --settings \
  SPRING_PROFILES_ACTIVE=prod \
  ZELA_AMBIENTE=azure \
  FOTOS_DIRETORIO=/home/zela/uploads \
  CORS_ORIGENS='*' \
  MENSAGERIA_MODO=local \
  AGENDAMENTO_INTERVALO=PT5M
ok "Configurações não sensíveis aplicadas"

# ---------- Resumo ----------
DB_URL="jdbc:sqlserver://${SQL_SERVER}.database.windows.net:1433;database=${SQL_DB};encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30"
cat <<RESUMO

=====================================================================================
 Recursos prontos no Resource Group ${RG} (${LOCATION})
=====================================================================================
 API ........ https://${APP}.azurewebsites.net   (no ar após a 1ª Release)
 Swagger .... https://${APP}.azurewebsites.net/swagger-ui.html
 Azure SQL .. ${SQL_SERVER}.database.windows.net  ·  banco ${SQL_DB}  ·  admin ${SQL_ADMIN}

 Variable group "zela-secrets" no Azure DevOps (Pipelines > Library):
   DB_URL         = ${DB_URL}
   DB_USER        = ${SQL_ADMIN}
   DB_PASSWORD    = (senha do admin do SQL)             🔒 secreta
   JWT_SECRET     = (gere com: openssl rand -base64 48)  🔒 secreta
   GEMINI_API_KEY = (opcional; sem ela a IA é simulada)  🔒 secreta
=====================================================================================
RESUMO
