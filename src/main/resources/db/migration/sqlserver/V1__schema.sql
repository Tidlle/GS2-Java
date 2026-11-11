-- GERADO por docs/ferramentas/h2_para_sqlserver.py a partir de db/migration/h2/V1__schema.sql. Não edite à mão.
-- Zela API · schema inicial (SQL Server / Azure SQL)
-- Datas em UTC (DATETIME2 sem fuso). Enums gravados como texto.

CREATE TABLE executores (
    id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    tipo      VARCHAR(20)  NOT NULL,
    regiao    VARCHAR(80)  NOT NULL,
    contrato  VARCHAR(160) NOT NULL,
    CONSTRAINT ck_executor_tipo CHECK (tipo IN ('EQUIPE_PROPRIA', 'EMPRESA', 'CONCESSIONARIA'))
);

CREATE TABLE categorias (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    nome                    VARCHAR(60) NOT NULL,
    icone                   VARCHAR(40) NOT NULL,
    executor_responsavel_id BIGINT      NOT NULL,
    prazo_horas             INTEGER     NOT NULL,
    emissao_automatica      BIT     NOT NULL,
    risco_base              INTEGER     NOT NULL,
    CONSTRAINT uk_categoria_nome UNIQUE (nome),
    CONSTRAINT fk_categoria_executor FOREIGN KEY (executor_responsavel_id) REFERENCES executores (id),
    CONSTRAINT ck_categoria_prazo CHECK (prazo_horas > 0),
    CONSTRAINT ck_categoria_risco CHECK (risco_base BETWEEN 1 AND 5)
);

CREATE TABLE usuarios (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    papel       VARCHAR(20)  NOT NULL,
    bairro      VARCHAR(80),
    reputacao   INTEGER      NOT NULL,
    executor_id BIGINT,
    criado_em   DATETIME2    NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_executor FOREIGN KEY (executor_id) REFERENCES executores (id),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('CIDADAO', 'GESTOR', 'EXECUTOR', 'SUPERVISOR')),
    CONSTRAINT ck_usuario_reputacao CHECK (reputacao BETWEEN 0 AND 100)
);

CREATE TABLE ocorrencias (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    autor_id          BIGINT           NOT NULL,
    categoria_id      BIGINT           NOT NULL,
    titulo            VARCHAR(160)     NOT NULL,
    descricao_ia      VARCHAR(1000)    NOT NULL,
    bairro            VARCHAR(80)      NOT NULL,
    endereco          VARCHAR(200)     NOT NULL,
    latitude          FLOAT NOT NULL,
    longitude         FLOAT NOT NULL,
    foto_antes_url    VARCHAR(300),
    urgente           BIT          NOT NULL,
    status            VARCHAR(30)      NOT NULL,
    prioridade        INTEGER          NOT NULL,
    confirmacoes_sim  INTEGER          NOT NULL,
    confirmacoes_nao  INTEGER          NOT NULL,
    ordem_atual_id    BIGINT,
    orgao_encaminhado VARCHAR(120),
    protocolo_externo VARCHAR(60),
    motivo_descarte   VARCHAR(500),
    criada_em         DATETIME2        NOT NULL,
    atualizada_em     DATETIME2        NOT NULL,
    CONSTRAINT fk_ocorrencia_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ocorrencia_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_ocorrencia_status CHECK (status IN ('REPORTADA', 'EM_TRIAGEM', 'AGUARDANDO_CONFIRMACAO',
        'CONFIRMADA', 'EM_ATENDIMENTO', 'RESOLVIDA', 'URGENTE', 'DESCARTADA', 'ENCAMINHADA')),
    CONSTRAINT ck_ocorrencia_prioridade CHECK (prioridade BETWEEN 1 AND 5)
);

CREATE TABLE confirmacoes (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    ocorrencia_id BIGINT    NOT NULL,
    usuario_id    BIGINT    NOT NULL,
    ainda_existe  BIT   NOT NULL,
    criada_em     DATETIME2 NOT NULL,
    CONSTRAINT uk_confirmacao_usuario UNIQUE (ocorrencia_id, usuario_id),
    CONSTRAINT fk_confirmacao_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencias (id),
    CONSTRAINT fk_confirmacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE ordens_servico (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    ocorrencia_id     BIGINT      NOT NULL,
    executor_id       BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL,
    prazo_etapa       DATETIME2   NOT NULL,
    checkin_latitude  FLOAT,
    checkin_longitude FLOAT,
    foto_depois_url   VARCHAR(300),
    observacao        VARCHAR(500),
    confianca_ia      FLOAT,
    protocolo_externo VARCHAR(60),
    retrabalho        BIT     NOT NULL,
    cidadao_confirmou BIT,
    concluida_em      DATETIME2,
    criada_em         DATETIME2   NOT NULL,
    CONSTRAINT fk_os_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencias (id),
    CONSTRAINT fk_os_executor FOREIGN KEY (executor_id) REFERENCES executores (id),
    CONSTRAINT ck_os_status CHECK (status IN ('EMITIDA', 'ACEITA', 'RECUSADA', 'EM_EXECUCAO', 'CONCLUIDA',
        'VALIDADA', 'ENCERRADA', 'REABERTA', 'ESCALADA', 'ENCAMINHADA')),
    CONSTRAINT ck_os_confianca CHECK (confianca_ia IS NULL OR confianca_ia BETWEEN 0 AND 1)
);

-- Referência circular: a ocorrência aponta para a OS vigente.
ALTER TABLE ocorrencias
    ADD CONSTRAINT fk_ocorrencia_ordem_atual FOREIGN KEY (ordem_atual_id) REFERENCES ordens_servico (id);

CREATE TABLE eventos_os (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    ordem_id        BIGINT       NOT NULL,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20)  NOT NULL,
    usuario_id      BIGINT,
    autor_nome      VARCHAR(120) NOT NULL,
    justificativa   VARCHAR(500),
    data            DATETIME2    NOT NULL,
    CONSTRAINT fk_evento_ordem FOREIGN KEY (ordem_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_evento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE avaliacoes (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    ordem_id    BIGINT    NOT NULL,
    cidadao_id  BIGINT    NOT NULL,
    aprovada    BIT   NOT NULL,
    nota        INTEGER,
    comentario  VARCHAR(500),
    criada_em   DATETIME2 NOT NULL,
    CONSTRAINT fk_avaliacao_ordem FOREIGN KEY (ordem_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_avaliacao_cidadao FOREIGN KEY (cidadao_id) REFERENCES usuarios (id),
    CONSTRAINT ck_avaliacao_nota CHECK (nota IS NULL OR nota BETWEEN 1 AND 5)
);

CREATE TABLE notificacoes (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    destinatario_id BIGINT       NOT NULL,
    texto           VARCHAR(300) NOT NULL,
    lida            BIT      NOT NULL,
    criada_em       DATETIME2    NOT NULL,
    CONSTRAINT fk_notificacao_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuarios (id)
);

CREATE INDEX ix_ocorrencia_status ON ocorrencias (status);
CREATE INDEX ix_ocorrencia_autor ON ocorrencias (autor_id);
CREATE INDEX ix_os_executor ON ordens_servico (executor_id);
CREATE INDEX ix_os_status ON ordens_servico (status);
CREATE INDEX ix_evento_ordem ON eventos_os (ordem_id);
CREATE INDEX ix_notificacao_destinatario ON notificacoes (destinatario_id);
