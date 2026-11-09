# Zela API — zeladoria colaborativa com IA

> Global Solution 2026/2 · **CitizenTech** · FIAP · 2º ano ADS · disciplina **Java Advanced**
> Grupo: _[nome do grupo]_ · Integrantes: _[RM — nome completo — turma]_

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

## Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/info` | público | Nome, versão e ambiente da API |
| GET | `/actuator/health` | público | Health check |

_A lista completa (autenticação, ocorrências, ordens de serviço, indicadores) entra conforme as fases avançam. Veja `docs/plano-java.md`._

---

## Andamento

- [x] Fase 1 — esqueleto do projeto, perfis, Swagger, health check, Docker
- [ ] Fase 2 — domínio e banco
- [ ] Fase 3 — segurança JWT e primeiros CRUDs
- [ ] Fase 4 — ocorrências
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
