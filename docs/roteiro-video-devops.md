# Roteiro do vídeo · DevOps Tools & Cloud Computing (Zela)

> Requisitos do vídeo: **mínimo 720p** (grave em 1080p), **áudio claro**, **narração por voz** (sem legendas no lugar da fala), **gravação contínua, sem cortes**.
> Cada item obrigatório que faltar tira **20 pontos**; o CRUD sem SELECT no banco tira **30**. Duração estimada: 15 a 20 minutos (as pipelines levam ~3 min a CI e ~5 min a Release).

---

## Antes de gravar (checklist)

- [ ] **Agente rodando:** abra um PowerShell e execute `C:\agent\run.cmd` (deixe em `Listening for Jobs`). Sem ele, nenhuma pipeline roda.
- [ ] **API acordada:** abra https://app-zela-rm562259.azurewebsites.net/swagger-ui.html uma vez antes de gravar.
- [ ] **Portal Azure** logado (https://portal.azure.com), com o filtro de assinatura mostrando `2TDSA RM565399 PHILLIPO`.
- [ ] **Banco para os SELECTs:** SSMS (ou Query editor do portal) conectado em `sql-zela-rm562259.database.windows.net`, banco `zela`, usuário `zelaadmin` (senha em `C:\Users\educa\.zela\segredos-azure.env`).
  - Se o seu IP mudou, libere-o: Portal › `sql-zela-rm562259` › **Rede** › *Adicionar o endereço IPv4 do cliente* › Salvar (ou rode de novo `bash scripts/infra-azure.sh`).
- [ ] **Abas abertas:** README no Azure Repos · Azure Boards (Sprint 1) · Pipelines · Portal Azure (`rg-zela`) · Swagger · SSMS.
- [ ] **Swagger logado como gestor:** `POST /auth/login` com `gestor@zela.com` / `123456` e clique em **Authorize** com o token.
- [ ] OBS em 1080p, microfone testado, notificações do Windows desligadas.

---

## Roteiro

### 1. README: solução, conceito e arquitetura (≈ 2 min)
- Azure Repos › `Zela` › README › seção **DevOps e nuvem (Azure)**.
- Explique o problema (zeladoria urbana) e a solução (foto → IA → confirmação dos vizinhos → OS rastreável).
- Mostre o **desenho da arquitetura** e percorra os passos 0 a 6 da legenda.

### 2. Portal Azure: recursos criados pelo script (≈ 1 min)
- Mostre `scripts/infra-azure.sh` no repositório (Azure CLI).
- Portal › **Grupos de recursos › `rg-zela`**: `sql-zela-rm562259`, banco `zela`, `plan-zela`, `app-zela-rm562259`.
- Abra o Web App (Java 21, HTTPS, health check) e o banco (Basic, collation UTF-8).

### 3. Azure Boards: nova tarefa (≈ 1 min)
- Boards › Sprint 1 › no PBI **#4**, **+ Task**: *"Atualizar a descrição da API no Swagger"*. Atribua a você e mude para **In Progress**.

### 4. Nova branch e alteração no **código-fonte** (≈ 2 min)
- Na Task, **Development › Create a branch** → `feature/<id>-descricao-swagger` (já fica vinculada à Task).
- Repos › nessa branch › edite `src/main/java/br/com/zela/api/config/OpenApiConfig.java`: no texto da `.description(...)`, acrescente uma frase, por exemplo:
  ```java
  serviço até a solução. Publicada na Azure pelo Azure Pipelines. GS 2026/2 · CitizenTech · FIAP.""")
  ```
  *(é código Java, não o README; o teste só confere o título "Zela API")*
- Commit com a mensagem `feat: descrição da API no Swagger AB#<id>`.

### 5. Pull Request e merge na main (≈ 1 min)
- **Create a pull request** › vincule a Task › mostre as políticas da `main`: revisor obrigatório (você), work item vinculado, comentários.
- **Approve** › **Complete** (merge na `main`).

### 6. Pipeline de Build (CI) automática (≈ 3 min)
- Pipelines › `zela-ci`: a execução começou sozinha pelo merge (*Triggered by: merge na main*).
- Percorra as etapas: checkout → JDK 21 → **Maven verify** → testes → cobertura → artefato.
- Aba **Tests**: 137 testes passando. Aba **Code Coverage**: relatório do JaCoCo.
- **Artefato `drop`** (Published): `zela-api-1.0.0.jar` (o pacote) e a pasta `scripts/`.

### 7. Pipeline de Release (CD) automática (≈ 5 min)
- Pipelines › `zela-cd`: disparou sozinha pelo artefato novo (*Triggered by zela-ci*).
- Etapas: baixar o drop → conferir o artefato → **aplicar as variáveis de ambiente** (variable group `zela-secrets`, segredos com cadeado) → **deploy no App Service** → health check e `/info`.
- Pipelines › **Environments › producao**: histórico de deploys.
- Enquanto espera, mostre `azure-pipeline.yml`, `azure-pipeline-cd.yml` e o variable group (Library).

### 8. Alteração publicada na nuvem (≈ 30 s)
- Recarregue o Swagger: a nova frase aparece na descrição, no topo da página.

### 9. CRUD em duas tabelas com SELECT no banco (≈ 4 min) · **sem cortes**
Após **cada** operação no Swagger, rode o SELECT da tabela no SSMS (não use GET para provar a gravação):

```sql
SELECT id, nome, tipo, regiao, contrato FROM executores ORDER BY id DESC;
SELECT id, nome, icone, executor_responsavel_id, prazo_horas, emissao_automatica, risco_base FROM categorias ORDER BY id DESC;
```

| # | Swagger | Conferir no banco |
|---|---|---|
| 1 | `POST /executores` (JSON do README) | SELECT executores → linha nova |
| 2 | `POST /categorias` com o `executorResponsavelId` criado | SELECT categorias → linha nova |
| 3 | `GET /executores/{id}` e `GET /categorias/{id}` | (leitura) |
| 4 | `PUT /executores/{id}` (JSON de PUT do README) | SELECT executores → dados alterados |
| 5 | `PUT /categorias/{id}` | SELECT categorias → dados alterados |
| 6 | `DELETE /categorias/{id}` → 204 | SELECT categorias → linha sumiu |
| 7 | `DELETE /executores/{id}` → 204 | SELECT executores → linha sumiu |

### 10. Encerramento no Boards (≈ 1 min)
- Abra a Task criada no passo 3: mostre os links de **branch, commit, Pull Request e builds**.
- Mude a Task para **Done**.

---

## Depois de gravar
- Suba no YouTube (não listado), **assista inteiro** e confira cada item acima.
- Cole o link no PDF de entrega (`Grupo_rm562259.pdf`: folha de rosto, link da organização Azure DevOps e link do vídeo).
