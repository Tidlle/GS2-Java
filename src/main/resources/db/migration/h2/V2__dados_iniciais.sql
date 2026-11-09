-- Zela API · dados iniciais (os mesmos cenários do app mobile)
-- Datas relativas ao momento da migração: DATEADD(HOUR, -29, LOCALTIMESTAMP) = 29 horas atrás (UTC).
-- Senha de todos os usuários de teste: 123456 (hash BCrypt).

INSERT INTO executores (id, nome, tipo, regiao, contrato) VALUES
    (1, 'Equipe Luz Norte', 'EMPRESA', 'Zona Sul', 'Contrato 014/2025 — Iluminação'),
    (2, 'Via Segura Pavimentação', 'EMPRESA', 'Zona Sul', 'Contrato 022/2025 — Pavimentação'),
    (3, 'Equipe Limpeza Urbana (própria)', 'EQUIPE_PROPRIA', 'Vila Mariana', 'Equipe própria da subprefeitura'),
    (4, 'Calçadas & Cia', 'EMPRESA', 'Zona Sul', 'Contrato 031/2025 — Calçadas'),
    (5, 'Defesa Civil / Parques', 'EQUIPE_PROPRIA', 'Cidade toda', 'Plantão de árvores e emergências');

INSERT INTO categorias (id, nome, icone, executor_responsavel_id, prazo_horas, emissao_automatica, risco_base) VALUES
    (1, 'Iluminação', 'bulb-outline', 1, 48, TRUE, 2),
    (2, 'Buraco', 'alert-circle-outline', 2, 72, TRUE, 3),
    (3, 'Entulho', 'trash-outline', 3, 48, TRUE, 1),
    (4, 'Calçada', 'walk-outline', 4, 120, FALSE, 2),
    (5, 'Árvore', 'leaf-outline', 5, 24, FALSE, 4);

INSERT INTO usuarios (id, nome, email, senha_hash, papel, bairro, reputacao, executor_id, criado_em) VALUES
    (1, 'Ana Souza', 'cidadao@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'CIDADAO', 'Vila Mariana', 82, NULL, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (2, 'Carlos Lima', 'gestor@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'GESTOR', 'Subprefeitura Vila Mariana', 50, NULL, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (3, 'Equipe Luz Norte', 'executor@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'EXECUTOR', NULL, 50, 1, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (4, 'Marina Costa', 'supervisor@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'SUPERVISOR', NULL, 50, NULL, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (5, 'João Pereira', 'cidadao2@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'CIDADAO', 'Vila Mariana', 64, NULL, DATEADD(HOUR, -1500, LOCALTIMESTAMP)),
    (6, 'Via Segura Pavimentação', 'pavimentacao@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'EXECUTOR', NULL, 50, 2, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (7, 'Equipe Limpeza Urbana', 'limpeza@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'EXECUTOR', NULL, 50, 3, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (8, 'Calçadas & Cia', 'calcadas@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'EXECUTOR', NULL, 50, 4, DATEADD(HOUR, -2000, LOCALTIMESTAMP)),
    (9, 'Plantão Defesa Civil', 'plantao@zela.com', '$2a$10$iksJSf4.iw0O./gIwWBwDuAgtByiDvkTXnabEBSYocrgiY9ty45wS', 'EXECUTOR', NULL, 50, 5, DATEADD(HOUR, -2000, LOCALTIMESTAMP));

-- Ocorrências (ordem_atual_id é preenchido depois das OS)
INSERT INTO ocorrencias (id, autor_id, categoria_id, titulo, descricao_ia, bairro, endereco, latitude, longitude,
                         foto_antes_url, urgente, status, prioridade, confirmacoes_sim, confirmacoes_nao,
                         orgao_encaminhado, protocolo_externo, motivo_descarte, criada_em, atualizada_em) VALUES
    (1, 1, 1, 'Poste apagado na Rua Domingos de Morais', 'Poste de iluminação pública apagado, deixando o trecho escuro à noite.',
        'Vila Mariana', 'R. Domingos de Morais, 1200', -23.5884, -46.6352, NULL, FALSE, 'EM_ATENDIMENTO', 3, 4, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -30, LOCALTIMESTAMP), DATEADD(HOUR, -29, LOCALTIMESTAMP)),
    (2, 1, 2, 'Buraco grande na Av. Jabaquara', 'Buraco de aproximadamente 60 cm no asfalto, com risco para motos e carros.',
        'Vila Mariana', 'Av. Jabaquara, 850', -23.5920, -46.6362, NULL, FALSE, 'EM_ATENDIMENTO', 4, 7, 1,
        NULL, NULL, NULL, DATEADD(HOUR, -60, LOCALTIMESTAMP), DATEADD(HOUR, -50, LOCALTIMESTAMP)),
    (3, 1, 3, 'Entulho na calçada da Rua Vergueiro', 'Entulho de obra acumulado sobre a calçada, bloqueando a passagem.',
        'Vila Mariana', 'R. Vergueiro, 3100', -23.5875, -46.6345, NULL, FALSE, 'EM_ATENDIMENTO', 2, 3, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -100, LOCALTIMESTAMP), DATEADD(HOUR, -3, LOCALTIMESTAMP)),
    (4, 1, 5, 'Árvore caída bloqueando a via', 'Árvore de grande porte caída sobre a via, com fiação próxima. Risco à vida.',
        'Vila Mariana', 'R. Humberto I, 420', -23.5864, -46.6383, NULL, TRUE, 'URGENTE', 5, 0, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -2, LOCALTIMESTAMP), DATEADD(HOUR, -1, LOCALTIMESTAMP)),
    (5, 1, 4, 'Calçada quebrada em frente à escola', 'Calçada com lajotas soltas e desnível, risco de queda para pedestres.',
        'Vila Mariana', 'R. Pelotas, 98', -23.5935, -46.6304, NULL, FALSE, 'EM_ATENDIMENTO', 3, 5, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -210, LOCALTIMESTAMP), DATEADD(HOUR, -100, LOCALTIMESTAMP)),
    (6, 1, 4, 'Calçada com raiz levantando o piso', 'Raiz de árvore levantou o piso da calçada, dificultando a passagem de cadeirantes.',
        'Vila Mariana', 'R. Loefgren, 700', -23.5950, -46.6395, NULL, FALSE, 'CONFIRMADA', 3, 3, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -28, LOCALTIMESTAMP), DATEADD(HOUR, -20, LOCALTIMESTAMP)),
    (7, 5, 1, 'Lâmpada piscando na Praça da Árvore', 'Luminária com lâmpada falhando de forma intermitente.',
        'Vila Mariana', 'Praça da Árvore, s/n', -23.5900, -46.6343, NULL, FALSE, 'AGUARDANDO_CONFIRMACAO', 2, 1, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -5, LOCALTIMESTAMP), DATEADD(HOUR, -5, LOCALTIMESTAMP)),
    (8, 5, 2, 'Buraco na Rua Domingos de Morais', 'Buraco pequeno no asfalto próximo ao cruzamento.',
        'Vila Mariana', 'R. Domingos de Morais, 900', -23.5870, -46.6355, NULL, FALSE, 'AGUARDANDO_CONFIRMACAO', 2, 0, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -3, LOCALTIMESTAMP), DATEADD(HOUR, -3, LOCALTIMESTAMP)),
    (9, 1, 3, 'Lixo acumulado no ponto de ônibus', 'Sacos de lixo acumulados junto ao ponto de ônibus.',
        'Vila Mariana', 'Av. Lins de Vasconcelos, 1500', -23.5857, -46.6340, NULL, FALSE, 'DESCARTADA', 1, 0, 4,
        NULL, NULL, 'Negada pelos vizinhos: o problema já havia sido resolvido.', DATEADD(HOUR, -40, LOCALTIMESTAMP), DATEADD(HOUR, -30, LOCALTIMESTAMP)),
    (10, 1, 2, 'Buraco em via estadual', 'Buraco em rodovia de competência estadual (DER).',
        'Vila Mariana', 'Av. Jabaquara, km 2', -23.5970, -46.6370, NULL, FALSE, 'ENCAMINHADA', 3, 2, 0,
        'DER-SP', 'DER-2026-004871', NULL, DATEADD(HOUR, -74, LOCALTIMESTAMP), DATEADD(HOUR, -69, LOCALTIMESTAMP)),
    (11, 1, 1, 'Poste apagado na Rua Cubatão', 'Poste de iluminação apagado; já resolvido e aguardando sua confirmação.',
        'Vila Mariana', 'R. Cubatão, 300', -23.5880, -46.6350, NULL, FALSE, 'EM_ATENDIMENTO', 3, 6, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -90, LOCALTIMESTAMP), DATEADD(HOUR, -8, LOCALTIMESTAMP)),
    (12, 1, 3, 'Entulho na Rua França Pinto', 'Restos de poda e entulho acumulados junto ao meio-fio.',
        'Vila Mariana', 'R. França Pinto, 500', -23.5910, -46.6330, NULL, FALSE, 'RESOLVIDA', 1, 3, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -410, LOCALTIMESTAMP), DATEADD(HOUR, -300, LOCALTIMESTAMP)),
    (13, 5, 1, 'Luminária apagada na Rua Sena Madureira', 'Luminária pública apagada em trecho com pouco movimento à noite.',
        'Vila Mariana', 'R. Sena Madureira, 600', -23.5905, -46.6375, NULL, FALSE, 'EM_ATENDIMENTO', 3, 3, 0,
        NULL, NULL, NULL, DATEADD(HOUR, -27, LOCALTIMESTAMP), DATEADD(HOUR, -20, LOCALTIMESTAMP));

INSERT INTO ordens_servico (id, ocorrencia_id, executor_id, status, prazo_etapa, checkin_latitude, checkin_longitude,
                            foto_depois_url, observacao, confianca_ia, protocolo_externo, retrabalho,
                            cidadao_confirmou, concluida_em, criada_em) VALUES
    (1, 1, 1, 'EMITIDA', DATEADD(HOUR, 18, LOCALTIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, DATEADD(HOUR, -29, LOCALTIMESTAMP)),
    (2, 2, 2, 'ACEITA', DATEADD(HOUR, 30, LOCALTIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, DATEADD(HOUR, -58, LOCALTIMESTAMP)),
    (3, 3, 3, 'EM_EXECUCAO', DATEADD(HOUR, 6, LOCALTIMESTAMP), -23.5876, -46.6346, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, DATEADD(HOUR, -98, LOCALTIMESTAMP)),
    (4, 4, 5, 'ESCALADA', DATEADD(HOUR, -1, LOCALTIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, DATEADD(HOUR, -2, LOCALTIMESTAMP)),
    (5, 11, 1, 'CONCLUIDA', DATEADD(HOUR, 40, LOCALTIMESTAMP), -23.5880, -46.6351, NULL, 'Troca de reator e lâmpada.', 0.93, NULL, FALSE, NULL, DATEADD(HOUR, -8, LOCALTIMESTAMP), DATEADD(HOUR, -88, LOCALTIMESTAMP)),
    (6, 5, 4, 'REABERTA', DATEADD(HOUR, 20, LOCALTIMESTAMP), -23.5935, -46.6305, NULL, NULL, 0.41, NULL, TRUE, NULL, DATEADD(HOUR, -150, LOCALTIMESTAMP), DATEADD(HOUR, -200, LOCALTIMESTAMP)),
    (7, 10, 2, 'ENCAMINHADA', DATEADD(HOUR, 100, LOCALTIMESTAMP), NULL, NULL, NULL, NULL, NULL, 'DER-2026-004871', FALSE, NULL, NULL, DATEADD(HOUR, -72, LOCALTIMESTAMP)),
    (8, 12, 3, 'ENCERRADA', DATEADD(HOUR, -300, LOCALTIMESTAMP), -23.5910, -46.6331, NULL, 'Recolhido e varrido.', 0.96, NULL, FALSE, NULL, DATEADD(HOUR, -350, LOCALTIMESTAMP), DATEADD(HOUR, -400, LOCALTIMESTAMP)),
    (9, 13, 1, 'ACEITA', DATEADD(HOUR, 20, LOCALTIMESTAMP), NULL, NULL, NULL, NULL, NULL, NULL, FALSE, NULL, NULL, DATEADD(HOUR, -26, LOCALTIMESTAMP));

UPDATE ocorrencias SET ordem_atual_id = 1 WHERE id = 1;
UPDATE ocorrencias SET ordem_atual_id = 2 WHERE id = 2;
UPDATE ocorrencias SET ordem_atual_id = 3 WHERE id = 3;
UPDATE ocorrencias SET ordem_atual_id = 4 WHERE id = 4;
UPDATE ocorrencias SET ordem_atual_id = 6 WHERE id = 5;
UPDATE ocorrencias SET ordem_atual_id = 7 WHERE id = 10;
UPDATE ocorrencias SET ordem_atual_id = 5 WHERE id = 11;
UPDATE ocorrencias SET ordem_atual_id = 8 WHERE id = 12;
UPDATE ocorrencias SET ordem_atual_id = 9 WHERE id = 13;

-- Timeline (auditoria) de cada OS
INSERT INTO eventos_os (ordem_id, status_anterior, status_novo, usuario_id, autor_nome, justificativa, data) VALUES
    (1, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -29, LOCALTIMESTAMP)),

    (2, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -58, LOCALTIMESTAMP)),
    (2, 'EMITIDA', 'ACEITA', 6, 'Via Segura Pavimentação', NULL, DATEADD(HOUR, -50, LOCALTIMESTAMP)),

    (3, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -98, LOCALTIMESTAMP)),
    (3, 'EMITIDA', 'ACEITA', 7, 'Equipe Limpeza Urbana', NULL, DATEADD(HOUR, -90, LOCALTIMESTAMP)),
    (3, 'ACEITA', 'EM_EXECUCAO', 7, 'Equipe Limpeza Urbana', 'Check-in a 18 m do local', DATEADD(HOUR, -3, LOCALTIMESTAMP)),

    (4, NULL, 'EMITIDA', NULL, 'Sistema (urgência — plantão)', NULL, DATEADD(HOUR, -2, LOCALTIMESTAMP)),
    (4, 'EMITIDA', 'ESCALADA', NULL, 'Sistema', 'Prazo de aceite do plantão estourado', DATEADD(HOUR, -1, LOCALTIMESTAMP)),

    (5, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -88, LOCALTIMESTAMP)),
    (5, 'EMITIDA', 'ACEITA', 3, 'Equipe Luz Norte', NULL, DATEADD(HOUR, -80, LOCALTIMESTAMP)),
    (5, 'ACEITA', 'EM_EXECUCAO', 3, 'Equipe Luz Norte', 'Check-in a 9 m do local', DATEADD(HOUR, -10, LOCALTIMESTAMP)),
    (5, 'EM_EXECUCAO', 'CONCLUIDA', 3, 'Equipe Luz Norte', 'Foto do depois enviada', DATEADD(HOUR, -8, LOCALTIMESTAMP)),

    (6, NULL, 'EMITIDA', 2, 'Carlos Lima', NULL, DATEADD(HOUR, -200, LOCALTIMESTAMP)),
    (6, 'EMITIDA', 'ACEITA', 8, 'Calçadas & Cia', NULL, DATEADD(HOUR, -190, LOCALTIMESTAMP)),
    (6, 'ACEITA', 'EM_EXECUCAO', 8, 'Calçadas & Cia', 'Check-in a 12 m do local', DATEADD(HOUR, -160, LOCALTIMESTAMP)),
    (6, 'EM_EXECUCAO', 'CONCLUIDA', 8, 'Calçadas & Cia', 'Foto do depois enviada', DATEADD(HOUR, -150, LOCALTIMESTAMP)),
    (6, 'CONCLUIDA', 'REABERTA', 2, 'Carlos Lima', 'Reparo parcial: lajotas ainda soltas', DATEADD(HOUR, -100, LOCALTIMESTAMP)),

    (7, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -72, LOCALTIMESTAMP)),
    (7, 'EMITIDA', 'RECUSADA', 6, 'Via Segura Pavimentação', 'Fora da competência: via estadual', DATEADD(HOUR, -70, LOCALTIMESTAMP)),
    (7, 'RECUSADA', 'ENCAMINHADA', 2, 'Carlos Lima', 'Protocolo DER-2026-004871', DATEADD(HOUR, -69, LOCALTIMESTAMP)),

    (8, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -400, LOCALTIMESTAMP)),
    (8, 'EMITIDA', 'ACEITA', 7, 'Equipe Limpeza Urbana', NULL, DATEADD(HOUR, -380, LOCALTIMESTAMP)),
    (8, 'ACEITA', 'EM_EXECUCAO', 7, 'Equipe Limpeza Urbana', 'Check-in a 6 m do local', DATEADD(HOUR, -355, LOCALTIMESTAMP)),
    (8, 'EM_EXECUCAO', 'CONCLUIDA', 7, 'Equipe Limpeza Urbana', 'Foto do depois enviada', DATEADD(HOUR, -350, LOCALTIMESTAMP)),
    (8, 'CONCLUIDA', 'VALIDADA', NULL, 'Aceite tácito (IA 96%, sem contestação em 48h)', NULL, DATEADD(HOUR, -302, LOCALTIMESTAMP)),
    (8, 'VALIDADA', 'ENCERRADA', NULL, 'Sistema', NULL, DATEADD(HOUR, -300, LOCALTIMESTAMP)),

    (9, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, DATEADD(HOUR, -26, LOCALTIMESTAMP)),
    (9, 'EMITIDA', 'ACEITA', 3, 'Equipe Luz Norte', NULL, DATEADD(HOUR, -20, LOCALTIMESTAMP));

-- Votos individuais de vizinhos (os contadores das ocorrências também incluem votos anteriores)
INSERT INTO confirmacoes (ocorrencia_id, usuario_id, ainda_existe, criada_em) VALUES
    (1, 5, TRUE, DATEADD(HOUR, -29, LOCALTIMESTAMP)),
    (2, 5, TRUE, DATEADD(HOUR, -55, LOCALTIMESTAMP)),
    (9, 5, FALSE, DATEADD(HOUR, -35, LOCALTIMESTAMP)),
    (13, 1, TRUE, DATEADD(HOUR, -26, LOCALTIMESTAMP));

INSERT INTO notificacoes (destinatario_id, texto, lida, criada_em) VALUES
    (1, 'Sua ocorrência virou OS: "Poste apagado na Rua Domingos de Morais".', FALSE, DATEADD(HOUR, -29, LOCALTIMESTAMP)),
    (1, 'Confirme o problema perto de você: Lâmpada piscando na Praça da Árvore.', FALSE, DATEADD(HOUR, -5, LOCALTIMESTAMP)),
    (1, 'O problema da Rua Cubatão foi concluído. Confirme a solução.', TRUE, DATEADD(HOUR, -8, LOCALTIMESTAMP)),
    (4, 'Prazo estourado: OS #4 (árvore caída) foi escalada ao supervisor.', FALSE, DATEADD(HOUR, -1, LOCALTIMESTAMP)),
    (3, 'Nova OS #1: Poste apagado na Rua Domingos de Morais.', FALSE, DATEADD(HOUR, -29, LOCALTIMESTAMP));

-- Os próximos ids gerados continuam depois dos dados de exemplo.
ALTER TABLE executores ALTER COLUMN id RESTART WITH 100;
ALTER TABLE categorias ALTER COLUMN id RESTART WITH 100;
ALTER TABLE usuarios ALTER COLUMN id RESTART WITH 100;
ALTER TABLE ocorrencias ALTER COLUMN id RESTART WITH 100;
ALTER TABLE ordens_servico ALTER COLUMN id RESTART WITH 100;
