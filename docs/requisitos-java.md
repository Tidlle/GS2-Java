# Zela API — Conferência dos requisitos de Java Advanced

> Base: `2TDS Fevereiro - Global Solution 2026-2.pdf`, seção Java Advanced. Conferido em 09/11/2026.
> Números do projeto: 9 entidades, 53 endpoints, 135 testes automatizados, 93% das linhas cobertas por testes (JaCoCo).

## Tecnologias obrigatórias

| Exigida | Onde está |
|---|---|
| Spring Boot | Spring Boot 4.1.2 (`pom.xml`) |
| Spring Data JPA | Entidades em `dominio/`, repositórios em `repositorio/` (consultas derivadas, JPQL, `Specification`, `@EntityGraph`) |
| Spring Security com JWT | `seguranca/` (OAuth2 Resource Server, HS256, emissor conferido) |
| Spring AI | `ia/GeminiServicoIA` (Spring AI 2.0.1 + Google Gemini, multimodal e saída estruturada) |

## Requisitos (100 pts)

| # | Requisito | Pts | Evidência | Testes |
|---|---|---|---|---|
| 01 | API REST do domínio: ≥ 3 entidades relacionadas, CRUD com Spring Data JPA e paginação | 20 | 9 entidades com 1:N, N:1 e `@Embedded` (README, "Modelo de dados"). CRUD completo de **Categoria**, **Executor** e **Ocorrência** (o cidadão edita e exclui a própria). Todas as listagens com `Pageable` e resposta `{content, page}` | `CategoriaApiTest`, `ExecutorApiTest`, `OcorrenciaApiTest`, `MapeamentoJpaTest` |
| 02 | Bean Validation nos DTOs e `@RestControllerAdvice` com respostas padronizadas | 10 | DTOs `record` com `@NotBlank`, `@Size`, `@Min/@Max`, `@DecimalMin/Max`, `@Email` e mensagens em português. `excecao/TratadorGlobalDeErros` responde sempre `ProblemDetail` com a lista `erros` por campo (400, 401, 403, 404, 409, 422, 429, 500) | `AutenticacaoTest` (400 com lista, JSON malformado), testes de 404/409/422 em todas as classes de API |
| 03 | Spring Security + JWT, ≥ 2 papéis, endpoints protegidos por papel | 20 | 4 papéis (CIDADAO, GESTOR, EXECUTOR, SUPERVISOR) com `@PreAuthorize` em cada endpoint, mais conferência de **posse** (executor só nas OS da equipe, cidadão só nas próprias ocorrências). Senhas BCrypt, emissor do token conferido e bloqueio após 5 senhas erradas | `AutenticacaoTest` (token inválido, adulterado e de outro emissor; força bruta), 403 por papel e por posse em todas as classes |
| 04 | Spring AI integrado ao fluxo (não pode ser chat solto) | 20 | **Triagem**: a foto vira categoria, descrição e urgência, que definem executor, prazo e plantão. **Comparação** antes/depois na conclusão: a confiança da IA alimenta o **aceite tácito**. A resposta da IA é conferida (categoria existente, confiança de 0 a 1) e, se a IA falhar, a decisão passa para uma pessoa | `GeminiServicoIATest` (prompt com imagem e conversão da resposta), `TriagemServiceTest`, `FotoEIaApiTest`, `PrazoApiTest` (aceite tácito) |
| 05 | Arquitetura distribuída — **opção A: RabbitMQ** (evento publicado e consumido de forma assíncrona) | 20 | Eventos de domínio publicados no exchange `zela.eventos` **depois do commit**. Duas filas com consumidores `@RabbitListener` (notificações e alertas), 3 tentativas e DLQ | `RabbitConfigTest` (topologia), `PublicadorDeEventosTest` (exchange, chave, broker fora do ar), `NotificacaoEventosTest` (evento vira aviso só após o commit) |
| 06 | Swagger/OpenAPI, README de como rodar, API consumida pelo app Mobile | 10 | springdoc em `/swagger-ui.html` com Bearer e descrições. README com como rodar, usuários de teste, endpoints, opção do requisito 5 e declaração de IA. DTOs com os mesmos campos dos tipos do app (`zela/src/types`) | `RotasPublicasTest` (OpenAPI e esquema Bearer). App consumindo a API: Fase 10 (primeira versão, testada na web) |

## O que tira pontos

| Item | Situação |
|---|---|
| Entrega fora do prazo ou fora do portal (−100) | Entregar até 19/11, 23h55 (Fase 11) |
| Zip sem o `.txt` de identificação (−10) | Fase 11 |
| Fora do tema CitizenTech (−40) | Zeladoria colaborativa (inspiração 03 da GS), ODS 9, 10 e 11 |
| Não compila ou não sobe seguindo o README (−30) | `./mvnw verify` verde. API sobe com H2, IA simulada e mensageria local, sem nada externo. **Testar o README num clone limpo** (Fase 9) |
| Problema grave de segurança ou arquitetura (−10 cada) | Revisão abaixo |

## Revisão de segurança

| Ponto | Como está |
|---|---|
| Segredos | Nenhum no código: JWT, banco, RabbitMQ e Gemini vêm de variáveis de ambiente. `.env` fora do Git. Sem `JWT_SECRET` (mínimo 32 caracteres), a API não sobe em produção |
| Senhas | Hash BCrypt; nunca aparecem em respostas nem no token |
| Login | Mensagem única para e-mail inexistente ou senha errada; bloqueio de 15 minutos após 5 erros (429) |
| Token | HS256, validade de 8 h, emissor conferido, sem sessão (stateless) |
| Autorização | Papel em todos os endpoints (`@PreAuthorize`) e posse conferida nos serviços (403) |
| Entrada | Bean Validation em todos os DTOs; consultas com parâmetros (sem SQL montado com texto do usuário) |
| Upload | Tipo conferido pelos bytes do arquivo, limite de 8 MB, nome gerado pelo servidor e leitura só de nomes válidos (sem path traversal); `X-Content-Type-Options: nosniff` |
| Erros | Sem stack trace nas respostas (só no log) |
| Container | Imagem roda com usuário sem privilégios de root |
| Exposição | Actuator só com `health` e `info`; console do H2 desligado em produção |

## Apresentação presencial (30%)

| Item | Pts | Onde mostrar |
|---|---|---|
| Demo ponta a ponta com o app Mobile | 20 | Roteiro: cidadão reporta com foto (IA sugere) → urgência → executor aceita, faz check-in e conclui → gestor aprova → cidadão vê a timeline → supervisor vê o escalonamento (`POST /prazos/executar`) |
| Modelagem e relacionamentos | 15 | README, "Modelo de dados"; máquina de estados nos enums `StatusOS` e `StatusOcorrencia` |
| Fluxo de autenticação JWT | 20 | README, "Autenticação"; `SecurityConfig`, `JwtConfig`, `TokenService` |
| Como a feature de Spring AI funciona | 15 | README, "Inteligência artificial"; `GeminiServicoIA` e `TriagemService` |
| Parte distribuída | 15 | README, "Mensageria"; painel do RabbitMQ em http://localhost:15672 |
| Como ajuda o cidadão | 15 | Retorno em cada etapa (notificações), confirmação que não bloqueia, transparência por bairro (ODS 10) |

## Pendências conhecidas

- Testar a IA com a chave real do Gemini e registrar os resultados com fotos reais.
- Testar com o RabbitMQ real (Docker) e tirar prints do painel.
- Criar as migrações e o `scripts/script-bd.sql` do banco da nuvem, quando o chat de DevOps definir o banco.
- Fase 10 (app Mobile): revisitar GPS real, URL da nuvem, teste no celular e commits do app.
