# Zela API — Plano da parte de Java

> Disciplina: **Java Advanced** · GS 2026/2 CitizenTech · entrega no Portal FIAP até **19/11/2026, 23h55** (sem prorrogação).
> Base: `2TDS Fevereiro - Global Solution 2026-2.pdf` (requisitos oficiais) e `Ideia_GS2/docs/zela.md` (especificação do Zela).
> Objetivo: API Spring Boot que substitui os mocks do app mobile (`Ideia_GS2/zela/src/services/`) e é a solução publicada na Azure pela disciplina de **DevOps**.

---

## 1. O que a GS exige

### 1.1 Java Advanced (nota da entrega: 100 pts)

Tecnologias obrigatórias: **Spring Boot, Spring Data JPA, Spring Security com JWT, Spring AI**. A API é a **mesma consumida pelo app Mobile** e precisa de **IA generativa que agregue valor real ao cidadão**.

| # | Requisito | Pts | Como o Zela atende |
|---|---|---|---|
| 01 | API REST do domínio: **≥ 3 entidades relacionadas**, **CRUD** com Spring Data JPA e **paginação** | 20 | 9 entidades; CRUD completo de Categoria, Executor e Ocorrência; `Pageable` em todas as listagens |
| 02 | **Bean Validation** nos DTOs e tratamento global com **@RestControllerAdvice**, respostas padronizadas | 10 | DTOs `record` com `@NotBlank`, `@Size`…; `ProblemDetail` para 400/401/403/404/409/422 |
| 03 | **Spring Security + JWT**, **≥ 2 papéis**, endpoints protegidos por papel | 20 | 4 papéis (CIDADAO, GESTOR, EXECUTOR, SUPERVISOR) com `@PreAuthorize` e checagem de posse |
| 04 | **Spring AI**: funcionalidade com LLM **integrada ao fluxo** (não pode ser chat genérico) | 20 | Triagem da foto (categoria, descrição, urgência) e comparação antes/depois na conclusão da OS |
| 05 | Arquitetura distribuída: **A) RabbitMQ/Kafka** (evento publicado e consumido de forma assíncrona) e/ou B) microsserviços | 20 | **Opção A, RabbitMQ**: eventos da OS → notificações e alertas |
| 06 | **Swagger/OpenAPI**, **README** explicando como rodar, API **consumida pelo app Mobile** | 10 | springdoc + README completo + `zela/src/services` trocado por Axios |

**O que tira pontos:** entrega fora do prazo ou fora do portal (−100) · zip sem o `.txt` de identificação (−10) · fora do tema CitizenTech (−40) · **projeto não compila ou não sobe seguindo o README (−30)** · **problema grave de segurança ou arquitetura, por ocorrência (−10)**.

**Entrega:** um `.zip` com o `.txt` de identificação (RM, nome completo e turma de cada integrante), o **link do repositório público no GitHub** e o **link do vídeo pitch**. O README precisa ter: como rodar, usuários de teste, endpoints principais, a opção escolhida no requisito 5, o link do deploy e a declaração de uso de IA.

**Vídeo pitch (até 5 min, link público ou não listado, também usado na premiação):** 0:00 problema com dado real → 0:45 solução → 1:30 **demonstração rodando** → 3:00 modelo de negócio → 4:00 impacto e ODS.

**Apresentação presencial (23 a 27/11, 30% da nota):** demo ponta a ponta com o app Mobile (20) · modelagem e relacionamentos (15) · fluxo JWT (20) · como a feature de Spring AI funciona (15) · parte distribuída (15) · como ajuda o cidadão (15).

### 1.2 DevOps: o que muda no repositório Java

A solução publicada será a **API Java**. O pipeline é feito no chat de DevOps, mas o repositório precisa nascer compatível:

| Exigência de DevOps | Impacto no projeto Java |
|---|---|
| Código no **Azure Repos (privado)**, main protegida, build só após merge via PR | Repo `GS2-Java` com dois remotes: **GitHub público** (Java/Mobile) e **Azure Repos privado** (DevOps) |
| `azure-pipeline.yml` na **raiz** | Build com `./mvnw verify` |
| **Testes automáticos no CI**, publicando artefato e testes | `mvn verify` gera o `.jar` e relatórios JUnit (`target/surefire-reports`) e **passa sem chave de IA e sem RabbitMQ** |
| Dockerfiles na pasta **`/dockerfiles`** | `dockerfiles/Dockerfile` (build multi-stage), com contexto na raiz |
| `script-bd.sql` (DDL) e scripts Azure CLI na pasta **`/scripts`** | `scripts/script-bd.sql` com o mesmo DDL das migrações Flyway |
| Banco na nuvem: container ou **PaaS (Azure SQL ou Oracle)** | Banco escolhido no chat de DevOps; driver e dialeto das migrações seguem essa escolha |
| **Variáveis de ambiente**, nenhuma senha no código (−20) | Tudo em `${VAR}`: banco, `JWT_SECRET`, RabbitMQ, chave da IA; `.env.example` sem segredos |
| Vídeo: **CRUD em ≥ 2 tabelas**, comprovado com **SELECT no banco** | CRUD de **Categoria** e **Executor** pronto cedo (Fase 3) para o DevOps testar |
| App e banco **fora do localhost** (−70) | API, RabbitMQ e banco na Azure (arquitetura definida no chat de DevOps) |
| Desenho macro da arquitetura | `docs/arquitetura.png` (serve ao README de Java também) |
| Alteração simulada **no código-fonte**, não no README | Ex.: mudar o texto de `GET /info` ou o raio do check-in |

### 1.3 Mobile: o que depende da API

- **CRUD via API com Axios/Fetch** (create, read, update, delete), com loaders e mensagens de erro → a API expõe CRUD de ocorrência para o cidadão.
- **Autenticação real**: cadastro, login, logout e rotas protegidas → `POST /auth/cadastro`, `POST /auth/login`, JWT.
- "É imprescindível que a aplicação backend esteja ativa durante o período de avaliação" → **API na Azure no ar até ~30/11** (atenção ao crédito da assinatura).

### 1.4 Outras disciplinas (fora do escopo deste chat)

- **IA:** a triagem e a comparação antes/depois do Spring AI (IA generativa + visão computacional) podem ser reaproveitadas, se alguém fizer essa disciplina.
- **Banco de Dados** (Oracle PL/SQL + MongoDB) e **.NET**: não exigem integração com a API. Como só Java e DevOps serão feitos, **prazos, escalonamento e aceite tácito ficam na API Java** (`@Scheduled`).

---

## 2. Decisões

| # | Decisão | Situação |
|---|---|---|
| 1 | Requisito 5 | **Opção A, RabbitMQ** (resolvido pelo PDF) |
| 2 | Banco | **H2** em dev/testes · banco do deploy **decidido no chat de DevOps** (proposta: Azure SQL). Precisa estar decidido **antes da Fase 2**, porque define o dialeto das migrações e do `script-bd.sql` |
| 3 | Provedor de IA | **Gemini** (decidido), com chave própria em `GEMINI_API_KEY`, pelo **starter nativo do Google GenAI** do Spring AI (`spring-ai-starter-model-google-genai`). **Modo simulado** automático sem chave |
| 4 | Serviço .NET | Não será feito → agendadores na API Java |
| 5 | Repositórios | GitHub público (entrega de Java, com o e-mail do professor) + Azure Repos privado (DevOps) |
| 6 | Deploy | **Decidido no chat de DevOps.** A API só precisa ser configurável por variáveis de ambiente e ter Dockerfile e health check |

---

## 3. Stack

- **Java 21** · **Spring Boot 4.1.2** (o Initializr não oferece mais a 3.5) · Maven Wrapper
- Spring Web, Data JPA, Validation, Security, Actuator, AMQP
- **JWT** com o **OAuth2 Resource Server** do Spring Security (Nimbus, HS256): o próprio Spring valida o token, sem filtro manual · senhas com **BCrypt**
- **Spring AI 2.0.1** com o starter do Google GenAI (Gemini): `ChatClient` com imagem + saída estruturada em `record`
- **springdoc-openapi 3.2.0**
- **RabbitMQ** · **Flyway** · H2 (dev/testes) · driver do banco do deploy (definido no chat de DevOps)
- **springdoc-openapi** (Swagger UI com Bearer) · Lombok
- Testes: JUnit 5, Mockito, MockMvc, `spring-security-test`
- Local: `docker-compose.yml` com API + RabbitMQ (e SQL Server opcional), para "subir seguindo o README" com um comando

---

## 4. Estrutura do repositório `GS2-Java`

```
GS2-Java/
├── README.md                    # tudo o que a entrega de Java exige (seção 11)
├── azure-pipeline.yml           # CI/CD (chat de DevOps)
├── pom.xml, mvnw, mvnw.cmd
├── docker-compose.yml           # API + RabbitMQ para rodar local
├── .env.example                 # variáveis sem valores reais
├── dockerfiles/Dockerfile       # build multi-stage do .jar
├── scripts/
│   ├── script-bd.sql            # DDL (mesmo conteúdo das migrações)
│   └── *.sh                     # Azure CLI (chat de DevOps)
├── docs/
│   ├── plano-java.md            # este plano
│   └── arquitetura.png          # desenho macro
├── postman/zela-api.postman_collection.json
└── src/main/java/br/com/zela/api/
    ├── config/        # OpenAPI, CORS, RabbitMQ, Jackson, agendamento
    ├── seguranca/     # JwtService, JwtFilter, SecurityConfig, UsuarioLogado
    ├── dominio/       # entidades JPA + enums + máquina de estados
    ├── repositorio/   # Spring Data (filtros e paginação)
    ├── servico/       # regras de negócio
    ├── ia/            # ServicoIA, SpringAiServico, IaSimulada
    ├── mensageria/    # eventos, publisher (after-commit) e consumers
    ├── armazenamento/ # fotos (disco local / volume)
    ├── agendamento/   # confirmação 24 h, escalonamento, aceite tácito
    ├── web/           # controllers + DTOs (records) + mapeadores
    └── excecao/       # @RestControllerAdvice com ProblemDetail
```

---

## 5. Modelo de domínio (espelha `zela/src/types/index.ts`)

| Entidade | Relacionamentos e destaques |
|---|---|
| `Usuario` | `papel` (enum), `bairro`, `reputacao`; N:1 `Executor` (só para EXECUTOR); senha BCrypt (nunca sai nos DTOs) |
| `Executor` | `tipo` (EQUIPE_PROPRIA, EMPRESA, CONCESSIONARIA), `regiao`, `contrato`; 1:N `Categoria`, 1:N `OrdemServico`; indicadores **calculados** |
| `Categoria` | N:1 `Executor` responsável; `prazoHoras`, `emissaoAutomatica`, `riscoBase` |
| `Ocorrencia` | N:1 `Usuario` (autor), N:1 `Categoria`; `@Embedded Localizacao(lat, lng)`; status, prioridade, urgente, foto, descrição da IA, protocolo externo |
| `Confirmacao` | N:1 `Ocorrencia`, N:1 `Usuario`; única por par → sem voto duplo |
| `OrdemServico` | 1:1 `Ocorrencia`, N:1 `Executor`; status, `prazoEtapa`, check-in, foto depois, `confiancaIA`, `retrabalho` |
| `EventoOS` | N:1 `OrdemServico`; timeline e auditoria (status anterior → novo, usuário, justificativa, data) |
| `Avaliacao` | N:1 `OrdemServico`, N:1 `Usuario`; cidadão aprova ou contesta |
| `Notificacao` | N:1 `Usuario`; criada pelos consumers do RabbitMQ |

**Máquina de estados** no enum `StatusOS` (transições permitidas). Toda mudança passa por `ordem.transicionar(novo, usuario, justificativa)`, que valida (→ **409** se inválida) e grava o `EventoOS`. O mesmo vale para `StatusOcorrencia`.

**Carga inicial** (Flyway `V2`) com os mesmos dados dos mocks: 4 usuários de teste (`cidadao@`, `gestor@`, `executor@`, `supervisor@zela.com`, senha `123456`), 5 categorias, 5 executores, ocorrências e OS em vários status, para a demo do mobile continuar igual.

---

## 6. Endpoints (1 para 1 com `zela/src/services/`)

| Papel | Método e rota | Substitui no mobile |
|---|---|---|
| Público | `POST /auth/login` → `{token, usuario}` · `POST /auth/cadastro` (sempre CIDADAO) | `autenticar`, `cadastrarCidadao` |
| Todos | `GET /usuarios/me` · `GET /info` (versão, usada na alteração simulada do DevOps) | sessão |
| Todos / Gestor | `GET /categorias` · **CRUD** `POST/PUT/DELETE /categorias` (GESTOR) · `GET /categorias/{id}/executor-sugerido` | `listarCategorias`, `sugerirExecutor` |
| Gestor/Sup. | **CRUD** `GET/POST/PUT/DELETE /executores` (paginado) | `listarExecutores` |
| Cidadão | `POST /ocorrencias/triagem` (multipart foto) → categoria, descrição, urgente | `triarFoto` |
| Cidadão | **CRUD**: `POST /ocorrencias` · `PUT /ocorrencias/{id}` e `DELETE /ocorrencias/{id}` (só o autor, antes de virar OS) | `criarOcorrencia` + editar/excluir (CRUD do Mobile) |
| Todos | `GET /ocorrencias?lat&lng&raio&status&page` (calcula `distanciaM`) · `GET /ocorrencias/{id}` | `listarOcorrencias`, `obterOcorrencia` |
| Cidadão | `GET /ocorrencias/minhas` · `GET /ocorrencias/para-confirmar` · `POST /ocorrencias/{id}/confirmacoes` | `listarMinhasOcorrencias`, `listarParaConfirmar`, `responderConfirmacao` |
| Gestor | `GET /ocorrencias/excecoes` · `POST /ocorrencias/{id}/emitir-os` · `/encaminhar` · `/descartar` | `listarExcecoes`, `emitirOS`, `encaminharOcorrencia`, `descartarOcorrencia` |
| Gestor/Sup. | `GET /ordens?status&executorId&bairro&atrasadas&page` · `GET /ordens/{id}` (com timeline) | `listarOrdens`, `obterOrdem` |
| Executor | `GET /ordens/minhas` · `POST /ordens/{id}/aceitar` · `/recusar` (motivo obrigatório) | `listarOrdensDoExecutor`, `aceitarOS`, `recusarOS` |
| Executor | `POST /ordens/{id}/checkin` (lat, lng → Haversine no servidor, raio 100 m) | `iniciarExecucao` |
| Executor | `POST /ordens/{id}/concluir` (multipart foto depois → IA compara) | `concluirOS`, `compararAntesDepois` |
| Gestor | `POST /ordens/{id}/aprovar` · `/reprovar` · `PATCH /ordens/{id}/prazo` · `POST /ordens/{id}/cancelar` | `aprovarOS`, `reprovarOS`, `editarPrazoOS`, `cancelarOS` |
| Cidadão | `POST /ordens/{id}/avaliacao` (confirma ou contesta) | `responderSolucao` |
| Supervisor | `GET /ordens/escalonadas` · `POST /ordens/{id}/cobrar` · `/reatribuir` | `listarEscalonadas`, `cobrarExecutor`, `reatribuirOS` |
| Todos | `GET /notificacoes` · `POST /notificacoes/lidas` | `listarNotificacoes`, `marcarNotificacoesLidas` |
| Gestor/Sup. | `GET /indicadores` · `/indicadores/executores` · `/indicadores/bairros` | `calcularIndicadores` |
| Todos | `GET /fotos/{arquivo}` | URLs das fotos |

Os DTOs de resposta usam **os mesmos nomes de campo dos tipos TypeScript** (`titulo`, `distanciaM`, `confirmacoesSim`, `timeline[].usuario`…). Assim, no mobile, muda só o corpo das funções de `services/`.

---

## 7. Regras de negócio

1. **Urgência**: urgente (cidadão ou IA) pula a confirmação e emite OS ao plantão com prazo de 4 h; evento `ocorrencia.urgente`.
2. **Confirmação que não bloqueia**: 3 "sim" → `CONFIRMADA`, prioridade +1 e reputação do autor +; 3 "não" → `DESCARTADA`; **sem resposta em 24 h segue sozinha** (agendador).
3. **Emissão automática**: categoria com `emissaoAutomatica` → OS para o executor do contrato; senão vai para a **fila de exceções**.
4. **Recusa, reprovação, descarte e cancelamento exigem justificativa** (Bean Validation → 400).
5. **Check-in** só a até 100 m do local (→ 422 com a distância); **conclusão** exige foto.
6. **Reprovação ou contestação** → `REABERTA`, `retrabalho = true`, novo prazo de 24 h.
7. **Prazo estourado** → `ESCALADA` + notificação ao supervisor e ao cidadão (agendador).
8. **Aceite tácito**: `CONCLUIDA` com `confiancaIA ≥ 0,8` e sem contestação em 48 h → `VALIDADA → ENCERRADA` (agendador).
9. **Posse**: executor só age nas próprias OS; cidadão só edita, exclui e avalia as próprias ocorrências (→ 403).

---

## 8. Spring AI (requisito 04)

- Interface `ServicoIA`:
  - `triar(foto)` → `record Triagem(String categoria, String descricao, boolean urgente, double confianca)`
  - `comparar(fotoAntes, fotoDepois)` → `record Comparacao(boolean resolvido, double confianca, String observacao)`
- `SpringAiServico`: `ChatClient` + imagem (`Media`) + `.entity(Triagem.class)`; o prompt traz as categorias do banco e manda responder só com elas.
- **Gemini** pelo starter nativo do Google GenAI do Spring AI (já configurado no `application.yml`):
  ```yaml
  spring.ai.model.chat: ${IA_PROVEDOR:none}          # none = IA simulada · google-genai = Gemini
  spring.ai.google.genai.api-key: ${GEMINI_API_KEY:}
  spring.ai.google.genai.chat.options.model: ${IA_MODELO:gemini-2.5-flash}
  ```
  O modelo fica em variável (`IA_MODELO`); na Fase 6 conferimos qual modelo multimodal está disponível na chave.
- `IaSimulada`: ativa com `zela.ia.modo=simulada` ou sem chave. CI, testes e "rodar seguindo o README" funcionam sem custo e sem rede.
- Falha da IA (erro, timeout ou categoria inválida) → a ocorrência vai para `EM_TRIAGEM` (fila de exceções) e o cadastro não falha.
- Por que não é chat genérico: a resposta da IA **decide o fluxo** (categoria → executor e prazo; urgência → plantão; confiança → aceite tácito).

## 9. RabbitMQ (requisito 05, opção A)

- Exchange topic `zela.eventos` · routing keys `os.emitida`, `os.status`, `os.escalada`, `ocorrencia.urgente`.
- Publicação **após o commit** (`@TransactionalEventListener(AFTER_COMMIT)` → `RabbitTemplate`).
- Consumers assíncronos: `NotificacaoConsumer` (cria `Notificacao` para cidadão, executor e supervisor) e `AlertaConsumer` (log de urgências e escalonamentos).
- DLQ e mensagens em JSON (`Jackson2JsonMessageConverter`).
- Testes de integração rodam sem broker (publisher simulado); o fluxo real aparece no vídeo e na apresentação (painel do RabbitMQ).

## 10. Segurança e configuração (evitar −10 por problema grave e −20 do DevOps)

- JWT stateless, `@PreAuthorize`, respostas 401/403 em JSON; senha nunca exposta; segredo do JWT só por variável de ambiente.
- CORS liberado para o app Expo.
- Perfis: `dev` (H2 + IA simulada + RabbitMQ local), `test` (H2, sem broker), `prod` (tudo por variável: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `RABBITMQ_HOST/USER/PASSWORD`, `GEMINI_API_KEY`, `IA_MODELO`). A chave do Gemini **nunca** vai para o repositório: local em `.env` (no `.gitignore`), no deploy como variável secreta do pipeline.
- Actuator `/actuator/health` para o health check do deploy.
- Upload: limite de tamanho, só `image/jpeg` e `image/png`, nome gerado pelo servidor.

## 11. Testes

- Unidade: máquina de estados da OS, Haversine e raio do check-in, regras de confirmação, aceite tácito, escalonamento.
- Integração (MockMvc + H2 + IA simulada): login, cadastro, 401/403 por papel, validações 400, CRUD paginado de categoria e executor, **fluxo completo** foto → OS → aceite → check-in → conclusão → aprovação.
- `./mvnw verify` verde sem serviços externos: é o que o CI do DevOps roda e publica.

## 12. README (itens cobrados)

Descrição do problema e da solução · ODS · **desenho da arquitetura** · **como rodar** (Docker Compose e Maven, passo a passo testado do zero) · **usuários de teste** · **endpoints principais** e link do Swagger · **opção do requisito 5 (A, RabbitMQ)** e como ver os eventos · **link do deploy** · link do vídeo pitch · integrantes · **declaração de uso de IA** (ferramentas e para quê).

---

## 13. Fases e tarefas

Commits pequenos e com mensagens claras (o histórico de Git é avaliado no Mobile e no DevOps). Cada fase termina com `./mvnw verify` verde e um commit por item relevante.

### Fase 1 · Esqueleto do projeto (09–10/11)
- [x] Gerar o projeto no Spring Initializr (Java 21, Boot 4.1.2, Maven) com Web, Data JPA, Validation, Security, OAuth2 Resource Server, Actuator, RabbitMQ, Flyway, H2, Lombok, Spring AI Google GenAI
- [x] Adicionar `springdoc-openapi` e o `build-info` (versão em `/info`)
- [x] Pacote base `br.com.zela.api` e a estrutura de pastas da seção 4
- [x] `application.yml` com perfis `dev`, `test` e `prod`; tudo sensível em `${VAR}`
- [x] Swagger em `/swagger-ui.html`, Actuator `/actuator/health`, `GET /info`
- [x] `dockerfiles/Dockerfile` (multi-stage) e `docker-compose.yml` (API + RabbitMQ)
- [x] `.gitignore` (inclui `.env`), `.env.example`, README inicial
- [x] `git init` e commits da Fase 1
- [x] Repositório público no GitHub: https://github.com/Tidlle/GS2-Java (remote `origin`)
- [ ] Validar `docker compose up` (Docker Desktop não respondeu em 09/11; a API já foi validada com `java -jar`)
- **Pronto quando:** `docker compose up` sobe a API e o Swagger abre

### Fase 2 · Domínio e banco (10/11) · *depende do banco escolhido no chat de DevOps*
- [x] Enums: `Papel`, `TipoExecutor`, `StatusOcorrencia`, `StatusOS` (com transições permitidas)
- [x] Entidades: Usuario, Executor, Categoria, Ocorrencia (`@Embedded Localizacao`), Confirmacao, OrdemServico, EventoOS, Avaliacao, Notificacao
- [x] Método `transicionar(...)` na OS e na ocorrência, gravando `EventoOS`
- [x] Repositórios Spring Data (consultas filtradas e paginadas entram com cada fase)
- [x] Flyway `V1__schema.sql` e `V2__dados_iniciais.sql` em `db/migration/h2` (dados dos mocks corrigidos, 9 usuários, senhas BCrypt)
- [ ] Migrações do banco da nuvem em `db/migration/{vendor}` (quando o DevOps definir o banco)
- [ ] `scripts/script-bd.sql` com o DDL do banco da nuvem (quando o DevOps definir o banco)
- [x] Testes de unidade da máquina de estados, da ocorrência e da distância + teste de mapeamento JPA com a carga inicial (33 testes no total)
- **Pronto quando:** a API sobe com H2 e os dados de teste aparecem no console do H2

### Fase 3 · Segurança, erros e primeiros CRUDs (11/11)
- [x] JWT pelo OAuth2 Resource Server (`JwtConfig`, `TokenService`), `SecurityConfig` stateless, BCrypt
- [x] `POST /auth/login`, `POST /auth/cadastro`, `GET /usuarios/me`
- [x] `@PreAuthorize` por papel; respostas 401/403 em JSON; CORS para o Expo
- [x] `@RestControllerAdvice` com `ProblemDetail` (400, 401, 403, 404, 409, 422)
- [x] DTOs `record` com Bean Validation (senha nunca sai na resposta)
- [x] **CRUD paginado de Categoria e Executor** (GESTOR)
- [x] Swagger com autenticação Bearer
- [x] Testes de integração: login, cadastro, 401/403, validação, CRUDs (60 testes no total)
- **Pronto quando:** dá para logar no Swagger com os 4 usuários e fazer o CRUD → **avisar o chat de DevOps que já pode montar o pipeline**

### Fase 4 · Ocorrências (11–12/11)
- [x] `POST /ocorrencias` (IA ainda simulada), `PUT` e `DELETE` (só o autor, antes de virar OS)
- [x] `GET /ocorrencias` com distância (Haversine), filtros e paginação; `GET /ocorrencias/{id}`, `/minhas`
- [x] Confirmação dos vizinhos (`/para-confirmar`, `/confirmacoes`), com voto único, prioridade e reputação
- [x] Caminho de urgência (OS direta ao plantão, prazo 4 h)
- [x] Emissão automática por categoria; fila de exceções
- [x] Gestor: `emitir-os`, `encaminhar` (protocolo), `descartar` (justificativa); `executor-sugerido`
- [x] Testes das regras de confirmação, urgência e emissão automática (82 testes no total)
- Obs.: a foto entra como `fotoAntesUrl` (texto); o upload chega na Fase 6

### Fase 5 · Ciclo da ordem de serviço (12–13/11)
- [ ] `GET /ordens` (filtros: status, executor, bairro, atrasadas) e `GET /ordens/{id}` com timeline
- [ ] Executor: `/minhas`, `aceitar`, `recusar` (motivo), `checkin` (raio de 100 m → 422 fora dele), `concluir` (foto)
- [ ] Gestor: `aprovar`, `reprovar` (retrabalho), `PATCH prazo`, `cancelar`
- [ ] Cidadão: `avaliacao` (confirma ou contesta → reabre)
- [ ] Supervisor: `escalonadas`, `cobrar`, `reatribuir`
- [ ] Checagem de posse (executor só nas próprias OS, cidadão só nas próprias ocorrências)
- [ ] Teste de integração do fluxo completo: ocorrência → OS → aceite → check-in → conclusão → aprovação

### Fase 6 · Fotos e Spring AI com Gemini (13–14/11)
- [ ] Upload de fotos (tipo e tamanho validados, nome gerado pelo servidor) e `GET /fotos/{arquivo}`
- [ ] Interface `ServicoIA` com `Triagem` e `Comparacao` (records)
- [ ] `SpringAiServico`: Gemini (starter Google GenAI), imagem + saída estruturada; prompt com as categorias do banco
- [ ] `IaSimulada` para dev, testes e CI (sem chave)
- [ ] `POST /ocorrencias/triagem` e comparação antes/depois no `concluir` (grava `confiancaIA`)
- [ ] Falha da IA → ocorrência em `EM_TRIAGEM` (fila de exceções)
- [ ] Testar com fotos reais (poste, buraco, entulho, calçada, árvore) e registrar os resultados para o README
- **Pronto quando:** com `GEMINI_API_KEY` definida, uma foto real vira categoria e descrição corretas

### Fase 7 · RabbitMQ (14/11)
- [ ] Exchange `zela.eventos`, filas, bindings e DLQ; conversor JSON
- [ ] Eventos de domínio publicados após o commit (`os.emitida`, `os.status`, `os.escalada`, `ocorrencia.urgente`)
- [ ] `NotificacaoConsumer` (cria notificações) e `AlertaConsumer` (log de urgência e escalonamento)
- [ ] `GET /notificacoes` e `POST /notificacoes/lidas`
- [ ] Testes sem broker (publisher simulado) + teste manual no painel do RabbitMQ (prints para o README)

### Fase 8 · Prazos e indicadores (15/11)
- [ ] `@EnableScheduling` com intervalos configuráveis (curtos na demo)
- [ ] Agendador de confirmação: 24 h sem resposta → segue o fluxo
- [ ] Agendador de escalonamento: prazo estourado → `ESCALADA` + evento
- [ ] Agendador de aceite tácito: confiança ≥ 0,8 e 48 h sem contestação → encerra
- [ ] `GET /indicadores`, `/indicadores/executores`, `/indicadores/bairros` (consultas agregadas)
- [ ] Testes de unidade dos três agendadores

### Fase 9 · Qualidade e testes (15–16/11)
- [ ] Revisar a cobertura dos requisitos 01–06 de Java item por item (seção 1.1)
- [ ] Completar testes de integração faltantes; `./mvnw verify` gera os relatórios JUnit para o CI
- [ ] Revisão de segurança: nenhum segredo no código, nenhuma senha em resposta, posse checada em todos os endpoints
- [ ] Coleção Postman com login e as rotas principais por papel

### Fase 10 · Integração com o app Mobile (16–17/11) · *em `Ideia_GS2/zela`*
- [ ] Instalar o Axios e criar o cliente com `baseURL` configurável e o token JWT no cabeçalho
- [ ] Trocar o login fictício pelo `/auth/login` e `/auth/cadastro` reais
- [ ] Reescrever o corpo das funções de `src/services/` chamando a API (telas iguais)
- [ ] Upload da foto (multipart) na nova ocorrência e na conclusão da OS
- [ ] Tratar erros da API (mensagens e loaders já existem nas telas)
- [ ] Testar o roteiro de demonstração inteiro contra a API publicada na Azure

### Fase 11 · Documentação e entrega (17–19/11)
- [ ] README completo (seção 12), testado do zero numa pasta limpa
- [ ] `docs/arquitetura.png` (desenho macro, também usado no DevOps)
- [ ] Declaração de uso de IA e link do deploy no README
- [ ] Gravar o **vídeo pitch** (até 5 min, roteiro da seção 1.1)
- [ ] E-mail do professor como colaborador ou leitor no GitHub
- [ ] Montar o `.zip`: `.txt` de identificação + link do repositório + link do vídeo
- [ ] **19/11 até 23h55:** entregar no Portal do Aluno

**Fora das fases, com data:** cadastro do grupo no formulário até **13/11**.

## 14. Riscos

| Risco | Mitigação |
|---|---|
| Migrações do banco do deploy não rodarem no H2 | H2 no modo de compatibilidade desse banco (ex.: `MODE=MSSQLServer`); se travar, pastas Flyway por banco (`db/migration/{vendor}`) |
| Banco do deploy decidido tarde | Decidir no chat de DevOps até 10/11; até lá a Fase 2 usa SQL portável (tipos e sintaxe comuns) |
| Cota gratuita do Gemini estourar ou o modelo mudar de nome | Modo simulado sempre disponível; gravar a demo com a IA real com antecedência |
| Crédito da Azure acabar antes da avaliação do Mobile (~30/11) | Recursos mínimos (ACI pequeno, Azure SQL tier básico/serverless) e desligar o que não for usado |
| "Não sobe seguindo o README" (−30) | Testar o README do zero em uma pasta limpa na Fase 11 |
| GitHub e Azure Repos fora de sincronia | Depois de cada merge no Azure: `git pull azure main` e `git push github main` |
