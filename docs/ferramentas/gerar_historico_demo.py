"""Gera o SQL do histórico de demonstração (OS já encerradas) para os indicadores."""
import sys

# categoria -> (executor, prazo em horas, usuario do executor)
CAT = {1: (1, 48, 3, "Iluminação"), 2: (2, 72, 6, "Buraco"), 3: (3, 48, 7, "Entulho"),
       4: (4, 120, 8, "Calçada"), 5: (5, 24, 9, "Árvore")}
EXEC_NOME = {1: "Equipe Luz Norte", 2: "Via Segura Pavimentação", 3: "Equipe Limpeza Urbana",
             4: "Calçadas & Cia", 5: "Plantão Defesa Civil"}
AUTOR_NOME = {1: "Ana Souza", 5: "João Pereira"}
BAIRRO = {"Vila Mariana": (-23.5893, -46.6347), "Saúde": (-23.6180, -46.6390),
          "Ipiranga": (-23.5890, -46.6060), "Jabaquara": (-23.6460, -46.6410)}

# (categoria, bairro, endereço, horas atrás da criação, duração até concluir, retrabalho, nota, autor)
CASOS = [
    (1, "Vila Mariana", "R. Joaquim Távora, 210", 900, 30, False, 5, 1),
    (1, "Saúde", "R. Carneiro da Cunha, 80", 820, 40, False, 4, 5),
    (1, "Ipiranga", "R. Bom Pastor, 1500", 760, 46, False, 5, 1),
    (1, "Jabaquara", "Av. Eng. Armando de Arruda Pereira, 900", 700, 70, False, 3, 5),
    (1, "Vila Mariana", "R. Afonso Celso, 300", 600, 22, False, 5, 1),
    (2, "Vila Mariana", "R. Sena Madureira, 1100", 880, 60, False, 4, 1),
    (2, "Saúde", "Av. Jabaquara, 1800", 780, 95, True, 2, 5),
    (2, "Ipiranga", "R. Silva Bueno, 700", 690, 70, False, 4, 1),
    (2, "Jabaquara", "R. das Rosas, 45", 610, 130, True, 2, 5),
    (3, "Vila Mariana", "R. Dr. Neto de Araújo, 120", 860, 20, False, 5, 1),
    (3, "Saúde", "R. Paracatu, 400", 740, 30, False, 5, 5),
    (3, "Ipiranga", "R. Agostinho Gomes, 90", 650, 44, False, 4, 1),
    (4, "Vila Mariana", "R. Domingos de Morais, 2500", 950, 110, False, 4, 1),
    (4, "Saúde", "R. Luís Góis, 500", 840, 170, True, 2, 5),
    (4, "Jabaquara", "R. Nelson Fernandes, 300", 720, 200, True, 1, 5),
    (5, "Vila Mariana", "R. Humberto I, 800", 800, 8, False, 5, 1),
    (5, "Ipiranga", "R. Costa Aguiar, 1000", 680, 15, False, 5, 1),
    (5, "Jabaquara", "Av. Cupecê, 2000", 560, 20, False, 4, 5),
]


def h(n):
    return f"DATEADD(MINUTE, {int(round(n * 60))}, LOCALTIMESTAMP)"


def sql(s):
    return "'" + s.replace("'", "''") + "'"


linhas = ["-- Zela API · histórico de demonstração (só em dev/prod; os testes não carregam este arquivo).",
          "-- OS já encerradas em quatro bairros, para os indicadores de desempenho e de bairro terem dados.",
          "-- Gerado por docs/ferramentas/gerar_historico_demo.py; datas relativas ao momento da migração.", ""]
oc, os_, ev, av = [], [], [], []
for i, (cat, bairro, end, atras, dur, retrab, nota, autor) in enumerate(CASOS):
    oid = 200 + i
    exe, prazo, usr, nome = CAT[cat]
    lat, lng = BAIRRO[bairro]
    lat += (i % 5 - 2) * 0.0011
    lng += (i % 3 - 1) * 0.0013
    criada_oc = -atras
    criada_os = -atras + 1
    concluida = criada_os + dur
    # Com retrabalho: concluiu, foi reaberta e concluiu de novo (conta como retrabalho).
    titulo = f"{nome}: {end}"
    oc.append(f"    ({oid}, {autor}, {cat}, {sql(titulo)}, {sql('Problema de ' + nome.lower() + ' resolvido.')}, "
              f"{sql(bairro)}, {sql(end)}, {lat:.4f}, {lng:.4f}, NULL, FALSE, 'RESOLVIDA', 2, 3, 0, "
              f"{h(criada_oc)}, {h(concluida + 10)})")
    os_.append(f"    ({oid}, {oid}, {exe}, 'ENCERRADA', {h(criada_os + prazo)}, {lat:.4f}, {lng:.4f}, "
               f"0.{88 if not retrab else 81}, {'TRUE' if retrab else 'FALSE'}, TRUE, {h(concluida)}, {h(criada_os)})")
    ev.append(f"    ({oid}, NULL, 'EMITIDA', NULL, 'Sistema (emissão automática)', NULL, {h(criada_os)})")
    ev.append(f"    ({oid}, 'EMITIDA', 'ACEITA', {usr}, {sql(EXEC_NOME[exe])}, NULL, {h(criada_os + 1)})")
    ev.append(f"    ({oid}, 'ACEITA', 'EM_EXECUCAO', {usr}, {sql(EXEC_NOME[exe])}, 'Check-in no local', {h(concluida - 2)})")
    if retrab:
        ev.append(f"    ({oid}, 'EM_EXECUCAO', 'CONCLUIDA', {usr}, {sql(EXEC_NOME[exe])}, 'Foto do depois enviada', {h(concluida - 30)})")
        ev.append(f"    ({oid}, 'CONCLUIDA', 'REABERTA', 2, 'Carlos Lima', 'Reparo incompleto', {h(concluida - 20)})")
        ev.append(f"    ({oid}, 'REABERTA', 'EM_EXECUCAO', {usr}, {sql(EXEC_NOME[exe])}, 'Retorno ao local', {h(concluida - 1)})")
    ev.append(f"    ({oid}, 'EM_EXECUCAO', 'CONCLUIDA', {usr}, {sql(EXEC_NOME[exe])}, 'Foto do depois enviada', {h(concluida)})")
    ev.append(f"    ({oid}, 'CONCLUIDA', 'VALIDADA', {autor}, {sql(AUTOR_NOME[autor])}, 'Cidadão confirmou a solução', {h(concluida + 10)})")
    ev.append(f"    ({oid}, 'VALIDADA', 'ENCERRADA', NULL, 'Sistema', NULL, {h(concluida + 10)})")
    av.append(f"    ({oid}, {autor}, TRUE, {nota}, NULL, {h(concluida + 10)})")

linhas.append("INSERT INTO ocorrencias (id, autor_id, categoria_id, titulo, descricao_ia, bairro, endereco, latitude, longitude,\n"
              "                         foto_antes_url, urgente, status, prioridade, confirmacoes_sim, confirmacoes_nao,\n"
              "                         criada_em, atualizada_em) VALUES")
linhas.append(",\n".join(oc) + ";\n")
linhas.append("INSERT INTO ordens_servico (id, ocorrencia_id, executor_id, status, prazo_etapa, checkin_latitude, checkin_longitude,\n"
              "                            confianca_ia, retrabalho, cidadao_confirmou, concluida_em, criada_em) VALUES")
linhas.append(",\n".join(os_) + ";\n")
linhas.append("UPDATE ocorrencias SET ordem_atual_id = id WHERE id BETWEEN 200 AND %d;\n" % (199 + len(CASOS)))
linhas.append("INSERT INTO eventos_os (ordem_id, status_anterior, status_novo, usuario_id, autor_nome, justificativa, data) VALUES")
linhas.append(",\n".join(ev) + ";\n")
linhas.append("INSERT INTO avaliacoes (ordem_id, cidadao_id, aprovada, nota, comentario, criada_em) VALUES")
linhas.append(",\n".join(av) + ";\n")
linhas.append("ALTER TABLE ocorrencias ALTER COLUMN id RESTART WITH 300;")
linhas.append("ALTER TABLE ordens_servico ALTER COLUMN id RESTART WITH 300;")
open(sys.argv[1], "w", encoding="utf-8", newline="\n").write("\n".join(linhas) + "\n")
print(len(CASOS), "casos")
