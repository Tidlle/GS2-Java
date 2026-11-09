# Zela API — zeladoria colaborativa com IA

> Global Solution 2026/2 · **CitizenTech** · FIAP · 2º ano ADS · disciplina **Java Advanced**
> Grupo: _[nome do grupo]_ · Integrantes: _[RM — nome completo — turma]_
> Repositório: https://github.com/Tidlle/GS2-Java · Deploy: _[link após o pipeline de DevOps]_ · Vídeo pitch: _[link]_

Buracos, postes apagados, entulho e calçadas quebradas ficam meses sem solução: o cidadão não sabe a quem recorrer, a mesma ocorrência é aberta várias vezes e a prefeitura não consegue fiscalizar quem executa o serviço.

O **Zela** transforma uma foto tirada pelo cidadão em uma **ordem de serviço rastreável de ponta a ponta**: a IA classifica o problema, os vizinhos confirmam, a OS vai para o executor responsável, que faz check-in no local e conclui com foto. A IA compara o antes e o depois, e o cidadão confirma ou contesta.

Esta é a **API principal**, consumida pelo app mobile (Expo/React Native) e publicada na Azure pela disciplina de DevOps.

**ODS:** 11 (cidades sustentáveis) · 10 (redução das desigualdades) · 9 (infraestrutura).

---

## Stack

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security, Actuator, AMQP) |
| Autenticação | Spring Security + JWT |
| IA generativa | Spring AI 2.0 + **Google Gemini** |
| Mensageria (requisito 5, opção A) | **RabbitMQ** |
| Banco | H2 (desenvolvimento e testes) · banco em nuvem (produção) · migrações com Flyway |
| Documentação | springdoc-openapi (Swagger UI) |
| Build | Maven Wrapper · Docker |

---

## Como rodar

### Opção 1 — Docker Compose (API + RabbitMQ)

Requisito: Docker.

```bash
docker compose up --build
```

### Opção 2 — Maven

Requisito: JDK 21. O RabbitMQ é opcional nesta fase (só é usado a partir da Fase 7).

```bash
./mvnw spring-boot:run
```

No Windows (PowerShell ou cmd), use `mvnw.cmd spring-boot:run`.

### Endereços

| O quê | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Informações da API | http://localhost:8080/info |
| Health check | http://localhost:8080/actuator/health |
| Console do H2 (dev) | http://localhost:8080/h2-console · JDBC URL `jdbc:h2:mem:zela` · usuário `sa`, sem senha |
| Painel do RabbitMQ | http://localhost:15672 · `guest` / `guest` |

### Testes

```bash
./mvnw verify
```

Os testes usam o perfil `test` e rodam **sem RabbitMQ e sem chave de IA**, como o pipeline de CI exige. Os relatórios JUnit ficam em `target/surefire-reports`.

---

## Configuração

Perfis: `dev` (padrão: H2 em memória, IA simulada), `test` (testes automatizados) e `prod` (nuvem: tudo por variável de ambiente).

Copie `.env.example` para `.env` para definir variáveis locais. O `.env` não vai para o Git.

| Variável | Uso | Obrigatória em prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil ativo (`dev`, `prod`) | sim |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão com o banco | sim |
| `JWT_SECRET` | Segredo do JWT (mínimo 32 caracteres) | sim |
| `JWT_EXPIRACAO_HORAS` | Validade do token (padrão 8) | não |
| `CORS_ORIGENS` | Origens liberadas para o navegador, separadas por vírgula (padrão `*`) | não |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Conexão com o RabbitMQ | sim |
| `IA_PROVEDOR` | `none` (IA simulada) ou `google-genai` (Gemini) | não |
| `GEMINI_API_KEY` | Chave da API do Gemini | se `IA_PROVEDOR=google-genai` |
| `IA_MODELO` | Modelo do Gemini (padrão `gemini-2.5-flash`) | não |
| `ZELA_AMBIENTE` | Nome do ambiente exibido em `/info` | não |

Nenhuma senha ou chave fica no código: os valores de produção vêm das variáveis do pipeline.

---

## Estrutura

```
├── azure-pipeline.yml        # CI/CD (DevOps)
├── docker-compose.yml        # API + RabbitMQ para rodar localmente
├── dockerfiles/Dockerfile    # imagem da API (build multi-stage)
├── scripts/                  # script-bd.sql e scripts Azure CLI (DevOps)
├── docs/                     # plano e desenho da arquitetura
└── src/main/java/br/com/zela/api/
    ├── config/        # OpenAPI, propriedades, CORS, RabbitMQ
    ├── seguranca/     # JWT e regras de acesso por papel
    ├── dominio/       # entidades JPA, enums e máquina de estados
    ├── repositorio/   # Spring Data JPA
    ├── servico/       # regras de negócio
    ├── ia/            # Spring AI (Gemini) e IA simulada
    ├── mensageria/    # eventos e consumidores do RabbitMQ
    ├── armazenamento/ # fotos
    ├── agendamento/   # prazos, escalonamento e aceite tácito
    ├── web/           # controllers e DTOs
    └── excecao/       # tratamento global de erros
```

---

## Usuários de teste

Criados pela carga inicial (Flyway `V2`). Senha de todos: **`123456`**.

| Papel | E-mail | Observação |
|---|---|---|
| Cidadão | `cidadao@zela.com` | Ana Souza, Vila Mariana |
| Cidadão | `cidadao2@zela.com` | João Pereira, autor de ocorrências aguardando confirmação |
| Gestor | `gestor@zela.com` | Carlos Lima, Subprefeitura Vila Mariana |
| Executor | `executor@zela.com` | Equipe Luz Norte (iluminação) |
| Supervisor | `supervisor@zela.com` | Marina Costa |
| Executor | `pavimentacao@zela.com` · `limpeza@zela.com` · `calcadas@zela.com` · `plantao@zela.com` | Um usuário para cada executor |

A carga traz os mesmos cenários do app: 13 ocorrências e 9 ordens de serviço cobrindo os status do ciclo (emitida, aceita, em execução, concluída, reaberta, escalada, encaminhada e encerrada), com a timeline de cada OS.

---

## Modelo de dados

```
Executor 1──N Categoria          (executor responsável pelo contrato da categoria)
Executor 1──N Usuario            (usuários do papel EXECUTOR)
Usuario  1──N Ocorrencia         (autor)
Categoria 1──N Ocorrencia
Ocorrencia 1──N Confirmacao N──1 Usuario   (um voto por usuário)
Ocorrencia 1──N OrdemServico N──1 Executor (ocorrencia.ordemAtual aponta a OS vigente)
OrdemServico 1──N EventoOS       (timeline e auditoria)
OrdemServico 1──N Avaliacao N──1 Usuario
Usuario 1──N Notificacao
```

- **Máquina de estados:** os enums `StatusOcorrencia` e `StatusOS` definem as transições permitidas. Toda mudança passa por `transicionar(...)`, que recusa transições inválidas e grava o evento na timeline (auditoria: quem, quando, de qual para qual status e por quê).
- **Localização** (`@Embedded`): latitude e longitude, com distância calculada pela fórmula de Haversine (usada no check-in da OS).
- **Schema:** criado pelo Flyway (`src/main/resources/db/migration/{banco}`); o Hibernate só valida (`ddl-auto: validate`). Datas gravadas em UTC.

---

## Autenticação (JWT)

1. `POST /auth/login` com e-mail e senha. A senha é conferida com o hash **BCrypt** guardado no banco.
2. A API devolve um **JWT** assinado com HMAC-SHA256 (segredo `JWT_SECRET`), válido por 8 horas. Claims: `sub` (id do usuário), `papel`, `nome`, `email`.
3. O app envia `Authorization: Bearer <token>` em todas as chamadas.
4. O **OAuth2 Resource Server** do Spring Security valida assinatura e validade e transforma o claim `papel` em `ROLE_<PAPEL>`.
5. Cada endpoint declara quem pode usá-lo com `@PreAuthorize` (ex.: `hasRole('GESTOR')`). Sem token: **401**; papel sem permissão: **403**.

No Swagger, faça login em `POST /auth/login`, copie o `token` e cole no botão **Authorize**.

## Regras do fluxo de ocorrências

| Situação | Como a API resolve |
|---|---|
| Reporte normal | Fica `AGUARDANDO_CONFIRMACAO`; prioridade inicial = risco da categoria |
| Emergência (`urgente: true`) | Pula a confirmação, prioridade máxima e OS imediata ao executor da categoria com prazo de plantão (4 h) |
| Vizinho confirma ("sim") | Prioridade +1 e reputação de quem reportou +1. Com 3 "sim", fica `CONFIRMADA` |
| Confirmada, categoria de rotina | OS emitida automaticamente ao executor do contrato, com o prazo contratual |
| Confirmada, categoria fora da regra | Vai para a fila de exceções do gestor (emitir, encaminhar ou descartar) |
| Vizinhos negam ("não") | Com 3 "não", `DESCARTADA` e reputação do autor −5 |
| Votos | Um por pessoa; quem reportou não vota na própria ocorrência |

Os limites ficam em `zela.regras` no `application.yml` (3 confirmações, 3 negações, 4 h de plantão, raio de 2 km).

## Erros

Todas as respostas de erro seguem o padrão **ProblemDetail** (RFC 9457), via `@RestControllerAdvice`:

```json
{
  "type": "https://zela.app/erros/validacao",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos",
  "instance": "/categorias",
  "timestamp": "2026-11-09T23:26:19Z",
  "erros": [ { "campo": "riscoBase", "mensagem": "O risco base vai de 1 a 5" } ]
}
```

| Status | Quando |
|---|---|
| 400 | Bean Validation nos DTOs, JSON malformado, parâmetro com tipo errado |
| 401 | Sem token, token inválido ou expirado, login recusado |
| 403 | Papel sem permissão |
| 404 | Recurso inexistente |
| 409 | Conflito: e-mail ou nome repetido, exclusão de registro em uso, transição de status inválida |
| 422 | Regra de negócio (ex.: check-in longe do local) |
| 500 | Erro inesperado (sem detalhes internos; stack trace só no log) |

## Endpoints

Listagens são paginadas: `?page=0&size=20&sort=nome,asc`. Resposta: `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/login` | público | Login; devolve o JWT e os dados do usuário |
| POST | `/auth/cadastro` | público | Cadastro de cidadão |
| GET | `/usuarios/me` | logado | Usuário dono do token |
| GET | `/categorias` · `/categorias/{id}` | logado | Lista (paginada) e detalhe das categorias |
| POST · PUT · DELETE | `/categorias` · `/categorias/{id}` | gestor | CRUD de categorias |
| GET | `/executores` · `/executores/{id}` | gestor, supervisor | Lista (paginada, filtro `nome`) e detalhe |
| POST · PUT · DELETE | `/executores` · `/executores/{id}` | gestor | CRUD de executores |
| POST | `/ocorrencias` | cidadão | Reporta um problema (urgente: OS imediata ao plantão) |
| GET | `/ocorrencias?lat&lng&raio&status` | logado | Lista; com posição, só as do raio e da mais próxima para a mais distante (`distanciaM`) |
| GET | `/ocorrencias/{id}` | logado | Detalhe |
| GET | `/ocorrencias/minhas` | cidadão | Reportadas por mim |
| PUT · DELETE | `/ocorrencias/{id}` | cidadão (autor) | Edita ou exclui a própria ocorrência antes de virar OS |
| GET | `/ocorrencias/para-confirmar` | cidadão | Ocorrências de vizinhos aguardando confirmação |
| POST | `/ocorrencias/{id}/confirmacoes` | cidadão | "Ainda está aqui?" sim/não |
| GET | `/ocorrencias/excecoes` | gestor | Fila de exceções (confirmadas sem emissão automática) |
| GET | `/categorias/{id}/executor-sugerido` | gestor, supervisor | Executor do contrato da categoria |
| POST | `/ocorrencias/{id}/emitir-os` | gestor | Emite a OS (executor e prazo opcionais) |
| POST | `/ocorrencias/{id}/encaminhar` | gestor | Encaminha a outro órgão com protocolo |
| POST | `/ocorrencias/{id}/descartar` | gestor | Descarta com justificativa |
| GET | `/info` | público | Nome, versão e ambiente da API |
| GET | `/actuator/health` | público | Health check |

_Ordens de serviço, notificações e indicadores entram nas próximas fases (veja `docs/plano-java.md`)._

---

## Andamento

- [x] Fase 1 — esqueleto do projeto, perfis, Swagger, health check, Docker
- [x] Fase 2 — domínio e banco (H2; migrações do banco da nuvem quando o DevOps definir)
- [x] Fase 3 — segurança JWT, tratamento de erros e CRUD de categorias e executores
- [x] Fase 4 — ocorrências, confirmação dos vizinhos e fila de exceções
- [ ] Fase 5 — ciclo da ordem de serviço
- [ ] Fase 6 — fotos e Spring AI (Gemini)
- [ ] Fase 7 — RabbitMQ
- [ ] Fase 8 — prazos e indicadores
- [ ] Fase 9 — qualidade e testes
- [ ] Fase 10 — integração com o app mobile
- [ ] Fase 11 — documentação e entrega

---

## Declaração de uso de IA

_[A preencher: ferramentas de IA generativa usadas no desenvolvimento e para quê, conforme a regra da GS.]_
