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
   │  (MENSAGERIA_MODO=local: eventos → notificações no próprio processo)    │
   └──────────────────────────────────────────────────────────────────────────┘
        ▲ HTTPS/JSON                                 │ HTTPS
   App mobile Expo (Zela)                       Google Gemini (IA)
```

**Por que assim**
- **App Service + Azure SQL** é o mesmo modelo que já funcionou na Sprint 4 (VitalPet): menos peças, deploy de `.jar` direto, HTTPS e domínio prontos, `/home` persistente para as fotos (`FOTOS_DIRETORIO=/home/zela/uploads`).
- **Sem ACI/ACR (decidido em 17/11):** na nuvem a API usa `MENSAGERIA_MODO=local` (os eventos viram notificações no próprio processo, depois do commit). Nenhuma disciplina exige o RabbitMQ na nuvem: Java cobra a mensageria no código (requisito 05) e ela é demonstrada localmente com `docker compose up` (painel em `localhost:15672`).
- O `dockerfiles/Dockerfile` continua no repo (roda local com `docker compose`) e pode ser validado na CI (`docker build`) como etapa extra.

**Custo estimado (17/11 → 30/11)**: App Service B1 ≈ US$ 6 · Azure SQL Basic ≈ US$ 2,5 → **≈ US$ 10** do crédito da assinatura usada.

---

## 3. Decisões em aberto (confirmar com o usuário)

| # | Decisão | Recomendação |
|---|---|---|
| D1 | Hospedagem | ✅ **Decidido:** App Service (API) + Azure SQL. ACI do RabbitMQ removido em 17/11 (`MENSAGERIA_MODO=local` na nuvem) |
| D2 | Release | **Dois pipelines YAML** (CI `azure-pipeline.yml` + CD `azure-pipeline-cd.yml` disparado pelo artefato da CI), como na Sprint 4. Alternativa: Release **Classic** (aba Releases, gatilho de CD no artefato) |
| D3 | Agente | Organização sem paralelismo hospedado (`total 0`). Pedir o **free grant** (2–3 dias úteis). Enquanto isso: **agente self-hosted `EDUARDO`** já registrado no pool `Default` (instalado em `C:\agent`, roda com `run.cmd`; precisa estar online nas execuções e no vídeo) |
| D4 | Professor | `profjoao.menk@fiap.com.br` (slide 17), já Basic na organização `RM5622590186` desde a Sprint 4 |

### Estado do Azure DevOps (Fase 0, 10/11)
- Organização: `https://dev.azure.com/RM5622590186` · Projeto **Zela** (privado, Scrum)
- Boards (Sprint 1 = 10/11 → 19/11): Epic **#2** › Feature **#3** › PBI **#4** › Task inicial **#5** "Configurar o projeto e publicar a Zela API na Azure" — usar `AB#5` nos commits da preparação
| D5 | Grupo | Individual ou grupo? (folha de rosto, RM do revisor padrão, nome do PDF) |
| D6 | Região | ✅ **`brazilsouth`**, permitida na assinatura do Phillipo (eastus, brazilsouth, centralus, mexicocentral, northcentralus). Reserva: `centralus` |
| D7 | Assinatura | ✅ A própria está `Disabled` desde 10/11. **Usando `2TDSA RM565399 PHILLIPO`**, com você como **Colaborador**. Para a service connection da Fase 6, o Phillipo precisa te dar **Proprietário só no `rg-zela`** |

---

## 4. Fases de execução

### Fase 0 — Azure DevOps e acessos (D0: 10–11/11)
- [x] Organização `RM5622590186` (a da Sprint 4) e **projeto privado** `Zela` (Scrum).
- [ ] ⚠️ Professor `profjoao.menk@fiap.com.br`: já é **Basic** na organização; **falta** incluí-lo em **Project Administrators** do projeto Zela (Project Settings › Permissions). Sem isso: −70.
- [x] Paralelismo hospedado não liberado; usando o agente self-hosted `EDUARDO`.
- [x] Boards: Epic #2 › Feature #3 › PBI #4 › Tasks #5 (preparação), #6 (CI), #7 (CD), #8 (documentação).

### Fase 1 — Preparar a API para o Azure SQL (11–12/11) · *mexe no código Java: alinhar com o chat de Java*
- [x] `pom.xml`: driver `mssql-jdbc` (runtime) e `flyway-sqlserver`.
- [x] Migrações `db/migration/sqlserver/V1`, `V2` e `db/demo/sqlserver/V2_1`, **geradas** a partir das do H2 por `docs/ferramentas/h2_para_sqlserver.py` (`BOOLEAN→BIT`, `TIMESTAMP→DATETIME2`, `DOUBLE PRECISION→FLOAT`, `IDENTITY(1,1)`, `TRUE/FALSE→1/0`, `LOCALTIMESTAMP→SYSUTCDATETIME()`, `SET IDENTITY_INSERT`, `DBCC CHECKIDENT`). Mudou um `.sql` do H2 → rodar o script de novo.
- [x] Textos continuam `VARCHAR`: o banco usa a collation **`Latin1_General_100_CI_AS_SC_UTF8`** (acentos/Unicode sem perda e validação do Hibernate sem ajustes). ⚠️ A Fase 3 deve criar o banco com `--collation Latin1_General_100_CI_AS_SC_UTF8`.
- [x] `application-prod.yml`: formato do `DB_URL` do Azure SQL e pool Hikari pequeno (`DB_POOL_MAXIMO`, padrão 5) para o tier Basic.
- [x] Validado (10/11) em SQL Server 2022 (container): Flyway aplicou V1, V2, V2.1; `ddl-auto: validate` passou; CRUD de Executor e Categoria (POST/PUT/GET/DELETE) conferido com SELECT, acentos corretos, ids novos a partir de 100, indicadores com médias corretas; log sem WARN/ERROR além do aviso do Swagger.
- [x] `./mvnw verify` verde só com H2: **135 testes, 0 falhas**.

### Fase 2 — `scripts/script-bd.sql` (12/11)
- [x] `scripts/script-bd.sql` gerado pelo mesmo `h2_para_sqlserver.py` (DDL da V1 + cabeçalho). Testado em banco vazio: 9 tabelas, FKs e CHECKs criados.

### Fase 3 — Infra via Azure CLI (12–13/11)
- [x] Escritos e testados com um `az` simulado (10/11) e **executados em 17/11** na assinatura do Phillipo (brazilsouth); a reexecução não duplica nada.
- [x] `scripts/infra-azure.sh` (Bash, roda no Cloud Shell ou Git Bash), idempotente e com variáveis no topo:
  - Resource Group `rg-zela`
  - Azure SQL Server + DB `zela` (Basic) + firewall "Allow Azure services" + IP do seu PC (para os SELECTs do vídeo)
  - App Service Plan Linux **B1** + Web App `app-zela-<rm>` runtime **JAVA:21-java21**, Always On, HTTPS obrigatório, health check `/actuator/health`
  - Azure SQL criado com `--collation Latin1_General_100_CI_AS_SC_UTF8`
  - App settings **não sensíveis** (perfil `prod`, `ZELA_AMBIENTE=azure`, `FOTOS_DIRETORIO=/home/zela/uploads`, `CORS_ORIGENS`, `MENSAGERIA_MODO=local`)
  - Senhas lidas por `read -s` ou variável de ambiente — **nunca** no arquivo
  - Saída final com URLs (API, Swagger, servidor SQL)
- [x] `scripts/delete-infra.sh` (`az group delete`, pede o nome do RG para confirmar) para depois de 30/11.
- [x] Recursos conferidos (17/11): `rg-zela` › `sql-zela-rm562259` + banco `zela` (Basic, UTF-8, Online) · `plan-zela` (B1 Linux) · `app-zela-rm562259` (Java 21, Running, HTTPS, Always On, health check). Conexão do PC ao banco testada com `sqlcmd`.
- [x] Senhas geradas em `C:\Users\educa\.zela\segredos-azure.env` (fora do Git, acesso só do seu usuário): `SQL_ADMIN_PASSWORD` e `JWT_SECRET`, para o variable group.

### Fase 4 — Azure Repos e políticas (13/11)
- [x] Varredura de segredos (17/11): 57 commits, nenhum `.env`, chave ou senha real no histórico.
- [x] Repo enviado (17/11): remote `azure` = `https://dev.azure.com/RM5622590186/Zela/_git/Zela`, branch padrão `main`. Os 5 commits com `AB#5` ficaram vinculados à Task #5 ("Fixed in Commit").
- [x] Branch policies na `main` (todas bloqueantes):
  - Minimum number of reviewers = **1**, autor pode aprovar o próprio PR (`creatorVoteCounts`)
  - **Check for linked work items = Required**
  - **Required reviewers** = RM562259 (revisor padrão, incluído automaticamente; voto do autor conta)
  - **Check for comment resolution**
  - (sem build validation: o build deve rodar só após o merge)
- [x] `scripts/azure-devops-policies.sh`: as mesmas políticas como código.
- ⚠️ **A `main` do Azure agora só recebe mudanças por PR.** Fluxo: branch `feature/<id>-...` → push `azure` → PR com work item → aprovar → merge. Depois: `git pull azure main` e `git push origin main` para manter o GitHub igual. Commits do chat de Java feitos direto na `main` local também precisam entrar no Azure por uma branch + PR.

### Fase 5 — Pipeline de Build/CI · `azure-pipeline.yml` ✅ (17/11)
- [x] Pipeline **`zela-ci`** (id 4) no agente self-hosted **EDUARDO** (pool `Default`; a organização não tem paralelismo hospedado). Autorizada a usar a fila `Default`; branch padrão `main`.
- [x] `trigger: main` (ignora `README.md`, `docs`, `postman`) e `pr: none`. Variáveis de ambiente: `MAVEN_OPTS`, `MAVEN_ARGS`, `IA_PROVEDOR`, `MENSAGERIA_MODO`, `versaoJava`, `artefato`.
- [x] Etapas: checkout → `JavaToolInstaller` (JDK 21) → `java -version` → **Maven verify** pelo wrapper (`.\mvnw.cmd` no Windows, `./mvnw` no Linux) → **PublishTestResults** (JUnit) → **PublishCodeCoverageResults** (JaCoCo) → CopyFiles (`.jar` e `scripts/`) → **PublishPipelineArtifact `drop`**.
- [x] Histórico: Task **#6** (Done) · PR **#1** (pipeline) → build **#28 falhou** (`'mvnw.cmd' não é reconhecido`: o agente iniciado pelo terminal do Claude herdou `NoDefaultCurrentDirectoryInExePath=1`) → PR **#2** (caminho explícito) → build **#29 ✅**: 137 testes JUnit passando, cobertura publicada, artefato `drop` com `zela-api-1.0.0.jar` (~99 MB) e `scripts/`.
- [ ] Melhoria (próximo PR): `mergeTestResults: true` para os 137 testes aparecerem em uma única execução na aba Tests (hoje são 19 execuções, uma por classe).
- ⚠️ **O agente precisa estar rodando** para qualquer build/release: `C:\agent\run.cmd` (deixe a janela aberta, inclusive no vídeo).

### Fase 6 — Pipeline de Release/CD · `azure-pipeline-cd.yml` ✅ (17/11)
- [x] Variable group **`zela-secrets`** (id 2): `DB_URL`, `DB_USER`, `IA_PROVEDOR`, `DB_PASSWORD`🔒, `JWT_SECRET`🔒 (segredos enviados do arquivo local, sem aparecer em lugar nenhum).
- [x] Service connection **`sc-zela-azure`**: workload identity federation, criação automática, papel **Colaborador só no `rg-zela`** (exigiu Proprietário no `rg-zela`, dado pelo Phillipo). Autorizada só para a `zela-cd`.
- [x] Pipeline **`zela-cd`** (id 5): `resources.pipelines` com `source: zela-ci` (dispara com o artefato novo da `main`), deployment job no environment **`producao`** (id 1). Autorizada na fila `Default`, no variable group e no environment.
- [x] Etapas: baixar o `drop` → conferir o artefato → **AzureAppServiceSettings** (variáveis do `zela-secrets`) → **AzureWebApp** (deploy do `.jar`, `JAVA|21-java21`) → PowerShell: `/actuator/health` = UP (até 8 min) e `/info`.
- [x] Histórico: Task **#7** (Done) · PR **#3** → CI **#30 ✅** → CD **#31 ✅** (parou num *Checkpoint.Authorization* do environment novo até autorizar a `zela-cd`). API no ar: health UP, Swagger 200, login OK; Azure SQL com as 9 tabelas e as migrações V1, V2 e V2.1.
- Branch padrão da `zela-cd` = `main` (o gatilho por artefato lê o YAML da branch padrão).

### Fase 7 — Ensaio ponta a ponta
- O fluxo completo (Task → branch → PR → merge → CI → CD) já rodou 3 vezes (PRs #1, #2 e #3). O roteiro do vídeo repete esse fluxo com uma alteração no código.
- [ ] CRUD de **Categoria** e **Executor** na nuvem com SELECT: fica para o vídeo (para o banco não ficar com dados de teste).

### Fase 8 — Documentação ✅ (17/11)
- [x] `docs/arquitetura-devops.svg` (fonte) + `.png` com **fluxo numerado (0–6) e legenda** (estilos de seta, cores das zonas, 🔒). PNG gerado por `docs/ferramentas/exportar-arquitetura.ps1` (Edge headless).
- [x] README — seção **DevOps e nuvem (Azure)**: solução e conceito, desenho, passos, recursos, scripts, Boards/Repos, pipelines, variáveis de ambiente, **CRUD em JSON** de Executor e Categoria (login, POST/GET/PUT/DELETE, ordem de exclusão) e SELECTs de conferência. Também: link do deploy no topo, Azure SQL na stack e `azure-pipeline-cd.yml` na estrutura.
- [x] `docs/roteiro-video-devops.md`: checklist antes de gravar e os 10 passos do vídeo. Alteração simulada: frase nova na descrição do Swagger (`OpenApiConfig.java`), que não quebra testes.

### Fase 9 — Vídeo (17/11)
Roteiro em `docs/roteiro-video-devops.md` (itens obrigatórios na seção 6). Gravar contínuo, 1080p, narrado; subir no YouTube (não listado) e **assistir inteiro** conferindo o checklist.

### Fase 10 — PDF e entrega (18/11, folga até 19/11)
- [ ] PDF `Grupo_rmXXXXX.pdf`: folha de rosto, link da organização Azure DevOps, link do YouTube.
- [ ] Conferir o acesso do professor em *Organization Settings → Users* (Basic) e *Project Settings → Permissions* (Project Administrators).
- [ ] Entregar no portal até 19/11 23h55.

### Pós-entrega (23–30/11)
- Manter API e banco no ar; ensaiar a apresentação (app rodando, arquitetura, perguntas sobre Boards/Repos/Pipelines).
- Depois da correção: `scripts/delete-infra.sh`.

---

## 5. Estrutura final do repositório (acréscimos de DevOps)

```
GS2-Java/
├── azure-pipeline.yml            # CI (raiz, nome exato)
├── azure-pipeline-cd.yml         # CD (se D2 = YAML)
├── dockerfiles/Dockerfile        # já existe
├── scripts/
│   ├── infra-azure.sh            # Azure CLI: RG, SQL, App Service
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
2. Portal Azure: recursos criados pelo script (RG, Azure SQL, App Service Plan, Web App).
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
