# Plano de DevOps — Zela API (GS 2026/2 · DevOps Tools & Cloud Computing)

> Objetivo da disciplina: publicar na Azure a solução de **Java Advanced** (a Zela API, este repositório) usando **Azure Boards + Azure Repos + Azure Pipelines**, com a infraestrutura criada por **script Azure CLI**.
> Entrega: **19/11/2026 até 23h55** (Portal FIAP, sem prorrogação) · Apresentação presencial: **23 a 27/11** (30% da nota) · API no ar até **~30/11**.

---

## 1. Requisitos oficiais (resumo do PDF da GS, slides de DevOps 1–17)

### 1.1 Tarefas obrigatórias
1. **Provisionamento via Azure CLI (script)**: Resource Group, banco, Web App/Container etc. O build e o deploy da release usam esses recursos.
2. **Projeto no Azure DevOps**: convidar o professor — Organização: **Basic**; Projeto: **Project Administrators** (confirmar o e-mail do professor; nos slides aparecem contas `@fiap.com.br`).
3. **Azure Repos**: importar o código-fonte da solução.
4. **Azure Boards**: tarefa inicial com **commits, branches e PRs vinculados**.
5. **Pipeline de Build (CI)**: roda a cada commit na branch principal; publica **artefato (pacote)** e **testes** (JUnit).
6. **Pipeline de Release (CD)**: deploy automático na nuvem **após a CI gerar um novo artefato**.

### 1.2 Requisitos
| # | Requisito | Como o Zela atende |
|---|---|---|
| 01 | Projeto **privado**, Git | Projeto privado no Azure DevOps, repo Git |
| 02 | Boards vinculado ao Repos | `AB#<id>` nos commits + work item no PR |
| 03 | `main` protegida: revisor obrigatório, work item vinculado, **revisor padrão (seu RM)** | Branch policies (Fase 4) |
| 04 | Build **só após merge na main via PR** | `trigger: main`, `pr: none`, sem build validation no PR |
| 05 | Aluno pode aprovar o próprio PR | Policy "Allow requestors to approve their own changes" |
| 06 | Release automática após novo artefato | CD disparado pelo artefato da CI |
| 07 | Deploy: Container (ACI/ACR) **ou** Serviço de Aplicativo | **App Service (Web App Linux, Java 21)** |
| 08 | Banco: Container **ou** PaaS (Azure SQL/Oracle) | **Azure SQL Database (PaaS)** |
| 09 | Scripts de infra no repositório | pasta `/scripts` |
| 10 | `script-bd.sql` (DDL) em `/scripts` | `scripts/script-bd.sql` (mesmo DDL do Flyway, dialeto SQL Server) |
| 11 | Scripts Azure CLI em `/scripts` | `scripts/infra-azure.sh`, `scripts/delete-infra.sh` |
| 12 | Dockerfiles em `/dockerfiles` | `dockerfiles/Dockerfile` (já existe) |
| 13 | `azure-pipeline.yml` na **raiz** (nome exato, singular) | `azure-pipeline.yml` |
| 14 | CRUD em JSON no README (APIs sem Swagger) | Temos Swagger, mas o README traz JSON de Categoria e Executor mesmo assim |
| 15 | Variáveis de ambiente nas pipelines | Variable group com segredos marcados como secret |
| 16 | Desenho macro da arquitetura | `docs/arquitetura-devops.png` (+ fonte `.drawio`) |
| 17 | Testes automáticos no CI | `./mvnw verify` (JUnit + JaCoCo) publicados no pipeline |

### 1.3 Pontuação (70 pts entrega + 30 pts apresentação)
- Desenho da arquitetura macro (desenho, fluxo, **legenda**) — 10
- Boards e Repos (histórico, links / branch, commit, merge) — 10
- Pipeline **Build** (etapas, tarefas, testes) — 25
- Pipeline **Release** (etapas, tarefas) — 25
- Apresentação: app funcionando (10), explicar a arquitetura (10), perguntas sobre Azure DevOps (10)

### 1.4 O que tira pontos (checklist de risco)
| Penalidade | Pts | Mitigação |
|---|---|---|
| Sem código no repo / sem `azure-pipeline.yml` / app ou banco em localhost / sem acesso do professor / sem Azure DevOps | −70 cada | Seções 4–7; professor convidado no D0 |
| Vídeo sem narração ou com legenda no lugar da voz | −30 | Narrar ao vivo |
| Sem `script-bd.sql` | −10 | Fase 2 |
| Sem Dockerfile (se usar container) | −30 | Já existe em `/dockerfiles` |
| Sem JSON de CRUD (API sem Swagger) | −10 | JSON no README mesmo com Swagger |
| Sem scripts Azure CLI | −30 | Fase 3 |
| Item obrigatório faltando no vídeo | −20/item | Roteiro da seção 6 |
| Vídeo < 720p ou áudio ruim | −30 | Gravar em 1080p, testar microfone |
| **CRUD sem comprovar no banco (usar SELECT, não GET)** | −30 | SELECT no Query Editor do portal / SSMS após cada operação |
| Senhas no código / sem variáveis de ambiente | −20 | Tudo via `${VAR}`; varredura antes de importar o repo |
| `main` desprotegida | −20 | Branch policies |
| Não criar branch nova para a tarefa | −10 | `feature/<id>-...` |
| Usar só uma tabela | −30 | 9 tabelas; CRUD em **Categoria** e **Executor** |
| Alteração simulada só no README | −20 | Alterar código (ex.: `InfoController` / `/info`) |
| Falta pasta `scripts` ou `dockerfiles` | −10 | Ambas no repo |
| Sem testes automáticos no CI | −20 | Maven task com JUnit publicado |

### 1.5 Entrega
PDF `NomeDoGrupo_rmXXXXX.pdf` com: folha de rosto (grupo, RM e nome completo dos integrantes), **link da organização no Azure DevOps** e **link do vídeo no YouTube**.

---

## 2. Arquitetura proposta

```
Dev (VS Code/IntelliJ) ──push──► Azure Repos (main protegida, PR + work item)
                                     │ merge na main
                                     ▼
                      Azure Pipelines — CI (azure-pipeline.yml)
                      Maven verify → JUnit + JaCoCo → artefato "drop" (.jar)
                                     │ novo artefato
                                     ▼
                      Azure Pipelines — CD (release)
                      download drop → deploy Web App → app settings → smoke test
                                     │
   ┌─────────────────────────── Azure (rg-zela) ─────────────────────────────┐
   │  App Service Linux B1 (Java 21) ── JDBC ──► Azure SQL Database (Basic)  │
   │        │  AMQP                                                          │
   │        └──────────► ACI: RabbitMQ (imagem oficial rabbitmq:4.1)         │
   └──────────────────────────────────────────────────────────────────────────┘
        ▲ HTTPS/JSON                                 │ HTTPS
   App mobile Expo (Zela)                       Google Gemini (IA)
```

**Por que assim**
- **App Service + Azure SQL** é o mesmo modelo que já funcionou na Sprint 4 (VitalPet): menos peças, deploy de `.jar` direto, HTTPS e domínio prontos, `/home` persistente para as fotos (`FOTOS_DIRETORIO=/home/uploads`).
- **RabbitMQ em ACI** mantém a mensageria real (requisito de Java) na nuvem, sem Dockerfile próprio (imagem oficial). Plano B: `MENSAGERIA_MODO=local` na nuvem se o ACI der problema de cota.
- O `dockerfiles/Dockerfile` continua no repo (roda local com `docker compose`) e pode ser validado na CI (`docker build`) como etapa extra.

**Custo estimado (19/11 → 30/11, ~3 semanas)**: App Service B1 ≈ US$ 10 · Azure SQL Basic ≈ US$ 4 · ACI 0,5 vCPU/1 GB ≈ US$ 15 → **≈ US$ 30** do crédito Azure for Students. Criar alerta de orçamento.

---

## 3. Decisões em aberto (confirmar com o usuário)

| # | Decisão | Recomendação |
|---|---|---|
| D1 | Hospedagem | ✅ **Decidido (10/11):** App Service (API) + Azure SQL + ACI (RabbitMQ) |
| D2 | Release | **Dois pipelines YAML** (CI `azure-pipeline.yml` + CD `azure-pipeline-cd.yml` disparado pelo artefato da CI), como na Sprint 4. Alternativa: Release **Classic** (aba Releases, gatilho de CD no artefato) |
| D3 | Agente | Organização sem paralelismo hospedado (`total 0`). Pedir o **free grant** (2–3 dias úteis). Enquanto isso: **agente self-hosted `EDUARDO`** já registrado no pool `Default` (instalado em `C:\agent`, roda com `run.cmd`; precisa estar online nas execuções e no vídeo) |
| D4 | Professor | `profjoao.menk@fiap.com.br` (slide 17), já Basic na organização `RM5622590186` desde a Sprint 4 |

### Estado do Azure DevOps (Fase 0, 10/11)
- Organização: `https://dev.azure.com/RM5622590186` · Projeto **Zela** (privado, Scrum)
- Boards (Sprint 1 = 10/11 → 19/11): Epic **#2** › Feature **#3** › PBI **#4** › Task inicial **#5** "Configurar o projeto e publicar a Zela API na Azure" — usar `AB#5` nos commits da preparação
| D5 | Grupo | Individual ou grupo? (folha de rosto, RM do revisor padrão, nome do PDF) |
| D6 | Região | `brazilsouth`; se a policy do Azure for Students bloquear, `eastus2`/`canadacentral` |

---

## 4. Fases de execução

### Fase 0 — Azure DevOps e acessos (D0: 10–11/11)
- [ ] Organização (reaproveitar a da Sprint 4 ou criar nova) e **projeto privado** `Zela` (processo Scrum ou Agile).
- [ ] Convidar o professor: Organização **Basic**, Projeto **Project Administrators** (slides 16–17).
- [ ] Pedir paralelismo hospedado (D3) ou preparar o agente self-hosted.
- [ ] Boards: Epic/Feature/**Task inicial** "Publicar a Zela API na Azure" (os commits desta fase vão linkados a ela).

### Fase 1 — Preparar a API para o Azure SQL (11–12/11) · *mexe no código Java: alinhar com o chat de Java*
- [x] `pom.xml`: driver `mssql-jdbc` (runtime) e `flyway-sqlserver`.
- [x] Migrações `db/migration/sqlserver/V1`, `V2` e `db/demo/sqlserver/V2_1`, **geradas** a partir das do H2 por `docs/ferramentas/h2_para_sqlserver.py` (`BOOLEAN→BIT`, `TIMESTAMP→DATETIME2`, `DOUBLE PRECISION→FLOAT`, `IDENTITY(1,1)`, `TRUE/FALSE→1/0`, `LOCALTIMESTAMP→SYSUTCDATETIME()`, `SET IDENTITY_INSERT`, `DBCC CHECKIDENT`). Mudou um `.sql` do H2 → rodar o script de novo.
- [x] Textos continuam `VARCHAR`: o banco usa a collation **`Latin1_General_100_CI_AS_SC_UTF8`** (acentos/Unicode sem perda e validação do Hibernate sem ajustes). ⚠️ A Fase 3 deve criar o banco com `--collation Latin1_General_100_CI_AS_SC_UTF8`.
- [x] `application-prod.yml`: formato do `DB_URL` do Azure SQL e pool Hikari pequeno (`DB_POOL_MAXIMO`, padrão 5) para o tier Basic.
- [x] Validado (10/11) em SQL Server 2022 (container): Flyway aplicou V1, V2, V2.1; `ddl-auto: validate` passou; CRUD de Executor e Categoria (POST/PUT/GET/DELETE) conferido com SELECT, acentos corretos, ids novos a partir de 100, indicadores com médias corretas; log sem WARN/ERROR além do aviso do Swagger.
- [x] `./mvnw verify` verde só com H2: **135 testes, 0 falhas**.

### Fase 2 — `scripts/script-bd.sql` (12/11)
- [ ] DDL completo das 9 tabelas no dialeto SQL Server (mesmo conteúdo do V1 do Flyway) + comentário explicando que o Flyway aplica automaticamente no deploy.

### Fase 3 — Infra via Azure CLI (12–13/11)
- [ ] `scripts/infra-azure.sh` (Bash, roda no Cloud Shell ou Git Bash), idempotente e com variáveis no topo:
  - Resource Group `rg-zela`
  - Azure SQL Server + DB `zela` (Basic) + firewall "Allow Azure services" + IP do seu PC (para os SELECTs do vídeo)
  - App Service Plan Linux **B1** + Web App `app-zela-<rm>` runtime **JAVA:21-java21**, Always On, health check `/actuator/health`
  - ACI `aci-zela-rabbitmq` (imagem `rabbitmq:4.1-management`, porta 5672, DNS label, usuário/senha por variável)
  - App settings **não sensíveis** (perfil `prod`, `ZELA_AMBIENTE=azure`, `FOTOS_DIRETORIO=/home/uploads`, `CORS_ORIGENS`, `RABBITMQ_HOST`)
  - Senhas lidas por `read -s` ou variável de ambiente — **nunca** no arquivo
  - Saída final com URLs (API, Swagger, servidor SQL)
- [ ] `scripts/delete-infra.sh` (`az group delete`) para depois de 30/11.
- [ ] Rodar o script e conferir os recursos no portal (vai aparecer no vídeo).
- [ ] Alerta de orçamento na assinatura.

### Fase 4 — Azure Repos e políticas (13/11)
- [ ] Varredura de segredos antes de subir (`.env` fora do Git, nada de chave/senha no histórico).
- [ ] Importar o repo: `git remote add azure <url-azure-repos>` e `git push azure main` (GitHub `origin` continua para Java/Mobile).
- [ ] Branch policies na `main`:
  - Minimum number of reviewers = **1** + **Allow requestors to approve their own changes**
  - **Check for linked work items = Required**
  - **Automatically included reviewers** = você (RM)
  - (sem build validation: o build deve rodar só após o merge)
- [ ] Opcional: `scripts/azure-devops-policies.sh` com `az repos policy ...` para documentar as políticas como código.

### Fase 5 — Pipeline de Build/CI (13–14/11) · `azure-pipeline.yml`
- `trigger: branches: [main]` (excluir `docs/**`, `*.md`), `pr: none`
- Variáveis: `MAVEN_OPTS`, versão do Java; segredos não são necessários na CI (testes usam H2 e IA simulada)
- Etapas/tarefas (o professor pontua "etapas, tarefas, testes"):
  1. Checkout
  2. `JavaToolInstaller`/JDK 21
  3. **Maven@4 `verify`** com `publishJUnitResults: true` (`**/surefire-reports/TEST-*.xml`)
  4. **PublishCodeCoverageResults@2** (JaCoCo `target/site/jacoco/jacoco.xml`)
  5. (opcional) `docker build -f dockerfiles/Dockerfile` para validar a imagem
  6. CopyFiles (`zela-api-*.jar` + `scripts/`) → **PublishPipelineArtifact `drop`**
- Conferir: aba **Tests** com os resultados, aba **Code Coverage**, artefato `drop` publicado.

### Fase 6 — Pipeline de Release/CD (14–15/11)
- Service connection Azure Resource Manager (workload identity) com a assinatura.
- Variable group `zela-secrets`: `DB_URL`, `DB_USER`, `DB_PASSWORD`🔒, `JWT_SECRET`🔒, `GEMINI_API_KEY`🔒, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`🔒 (pode ser ligado ao Key Vault como extra).
- Disparo automático pelo novo artefato da CI (D2).
- Etapas/tarefas:
  1. Download do artefato `drop`
  2. **AzureWebApp@1** deploy do `.jar` (Linux, Java 21)
  3. **AzureAppServiceSettings@1** com as variáveis do banco, JWT, RabbitMQ e IA vindas do variable group
  4. Smoke test: `curl` em `/actuator/health` (espera `UP`) e `/info` (mostra a versão publicada)
  5. Echo das URLs (API e Swagger)
- Usar `environment: producao` (histórico de deploys aparece no Azure DevOps).

### Fase 7 — Ensaio ponta a ponta (15/11)
- [ ] Task nova no Boards → branch `feature/<id>-...` → alteração no **código** (ex.: texto/versão do `GET /info`) → commit com `AB#<id>` → PR com work item → aprovar → merge → CI → CD → conferir na nuvem.
- [ ] CRUD de **Categoria** e **Executor** pelo Swagger na nuvem + `SELECT` no Azure SQL após cada operação.
- [ ] App mobile apontando para a API na Azure (opcional, enriquece a apresentação).

### Fase 8 — Documentação (16/11)
- [ ] `docs/arquitetura-devops.drawio` + `.png` com **fluxo numerado e legenda** (10 pts).
- [ ] README — seção DevOps: solução e conceito, desenho, recursos criados, como rodar os scripts, pipelines (CI/CD), variáveis de ambiente (sem valores), **JSON de CRUD** de Categoria e Executor (login → token → POST/GET/PUT/DELETE), SELECTs de conferência, links (Azure DevOps, API, Swagger).
- [ ] `docs/roteiro-video-devops.md`.

### Fase 9 — Vídeo (17/11)
Ver seção 6. Gravar contínuo, 1080p, narrado; subir no YouTube (não listado) e **assistir inteiro** conferindo o checklist.

### Fase 10 — PDF e entrega (18/11, folga até 19/11)
- [ ] PDF `Grupo_rmXXXXX.pdf`: folha de rosto, link da organização Azure DevOps, link do YouTube.
- [ ] Conferir o acesso do professor em *Organization Settings → Users* (Basic) e *Project Settings → Permissions* (Project Administrators).
- [ ] Entregar no portal até 19/11 23h55.

### Pós-entrega (23–30/11)
- Manter API, banco e RabbitMQ no ar; ensaiar a apresentação (app rodando, arquitetura, perguntas sobre Boards/Repos/Pipelines).
- Depois da correção: `scripts/delete-infra.sh`.

---

## 5. Estrutura final do repositório (acréscimos de DevOps)

```
GS2-Java/
├── azure-pipeline.yml            # CI (raiz, nome exato)
├── azure-pipeline-cd.yml         # CD (se D2 = YAML)
├── dockerfiles/Dockerfile        # já existe
├── scripts/
│   ├── infra-azure.sh            # Azure CLI: RG, SQL, App Service, ACI
│   ├── delete-infra.sh
│   ├── script-bd.sql             # DDL SQL Server
│   └── azure-devops-policies.sh  # opcional
├── src/main/resources/db/migration/sqlserver/…
├── src/main/resources/db/demo/sqlserver/…
└── docs/
    ├── plano-devops.md           # este plano
    ├── arquitetura-devops.drawio / .png
    └── roteiro-video-devops.md
```

---

## 6. Roteiro do vídeo (itens obrigatórios, −20 por item faltando)

1. README: solução, conceito e **desenho da arquitetura**.
2. Portal Azure: recursos criados pelo script (RG, SQL, App Service, ACI).
3. Azure Boards: **criar uma nova tarefa**.
4. **Criar branch nova** e alterar o **código-fonte** (não o README).
5. Commit vinculado, **PR** com work item, aprovação, **merge na main**.
6. Pipelines rodando **sozinhos**: Build (etapas, testes, cobertura, artefato) → Release (etapas, deploy).
7. Destacar artefatos publicados na CI (pacote, testes).
8. Mostrar a alteração publicada na nuvem (`/info`).
9. **CRUD completo em ≥ 2 tabelas** (Categoria, Executor) com **SELECT no banco** após cada operação.
10. Encerrar mostrando a tarefa concluída com links (commits, branch, PR, build).

Requisitos técnicos: mínimo 720p, áudio claro, narração por voz, gravação contínua.

---

## 7. Cronograma

| Data | Fase |
|---|---|
| ter 10/11 – qua 11/11 | 0 — Azure DevOps, professor, paralelismo · início da Fase 1 |
| qua 11/11 – qui 12/11 | 1 — API no Azure SQL · 2 — `script-bd.sql` |
| qui 12/11 – sex 13/11 | 3 — Infra Azure CLI · 4 — Repos e políticas |
| sex 13/11 – sáb 14/11 | 5 — CI |
| sáb 14/11 – dom 15/11 | 6 — CD · 7 — ensaio ponta a ponta |
| seg 16/11 | 8 — Arquitetura e README |
| ter 17/11 | 9 — Vídeo |
| qua 18/11 | 10 — PDF · folga |
| **qui 19/11 23h55** | **Entrega** |
| 23–27/11 | Apresentação presencial |
