"""Gera postman/zela-api.postman_collection.json (Postman Collection v2.1).

Uso (na raiz do projeto): python docs/ferramentas/gerar_postman.py
"""
import json
from pathlib import Path

PAPEIS = {
    "Cidadão": ("cidadao@zela.com", "tokenCidadao"),
    "Gestor": ("gestor@zela.com", "tokenGestor"),
    "Executor": ("executor@zela.com", "tokenExecutor"),
    "Supervisor": ("supervisor@zela.com", "tokenSupervisor"),
}


def req(nome, metodo, caminho, token=None, corpo=None, descricao="", testes=None, form=None):
    partes = caminho.split("?")
    url = {"raw": "{{baseUrl}}" + caminho, "host": ["{{baseUrl}}"],
           "path": [p for p in partes[0].split("/") if p]}
    if len(partes) > 1:
        url["query"] = [{"key": kv.split("=")[0], "value": kv.split("=")[1]} for kv in partes[1].split("&")]
    r = {"method": metodo, "header": [], "url": url, "description": descricao}
    if token:
        r["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": "{{%s}}" % token, "type": "string"}]}
    else:
        r["auth"] = {"type": "noauth"}
    if corpo is not None:
        r["header"].append({"key": "Content-Type", "value": "application/json"})
        r["body"] = {"mode": "raw", "raw": json.dumps(corpo, ensure_ascii=False, indent=2),
                     "options": {"raw": {"language": "json"}}}
    if form is not None:
        r["body"] = {"mode": "formdata", "formdata": [{"key": "arquivo", "type": "file", "src": []}]}
    item = {"name": nome, "request": r}
    if testes:
        item["event"] = [{"listen": "test", "script": {"type": "text/javascript", "exec": testes}}]
    return item


def pasta(nome, itens, descricao=""):
    return {"name": nome, "item": itens, "description": descricao}


login = [req(f"Login {papel}", "POST", "/auth/login", corpo={"email": email, "senha": "123456"},
             descricao=f"Guarda o token em {{{{{var}}}}}.",
             testes=["pm.test('login ok', () => pm.response.to.have.status(200));",
                     f"pm.collectionVariables.set('{var}', pm.response.json().token);"])
         for papel, (email, var) in PAPEIS.items()]
login.append(req("Cadastro de cidadão", "POST", "/auth/cadastro",
                 corpo={"nome": "Maria Oliveira", "email": "maria{{$randomInt}}@email.com", "senha": "segredo123",
                        "bairro": "Vila Mariana"}))
login.append(req("Usuário logado", "GET", "/usuarios/me", "tokenCidadao"))

guardar = lambda var, campo="id": [f"if (pm.response.code < 300) pm.collectionVariables.set('{var}', pm.response.json().{campo});"]

categorias = [
    req("Listar (paginado)", "GET", "/categorias?page=0&size=10&sort=nome,asc", "tokenCidadao"),
    req("Buscar", "GET", "/categorias/1", "tokenCidadao"),
    req("Criar", "POST", "/categorias", "tokenGestor",
        {"nome": "Sinalização", "icone": "warning-outline", "executorResponsavelId": 2, "prazoHoras": 72,
         "emissaoAutomatica": True, "riscoBase": 2}, testes=guardar("categoriaId")),
    req("Atualizar", "PUT", "/categorias/{{categoriaId}}", "tokenGestor",
        {"nome": "Sinalização viária", "icone": "warning-outline", "executorResponsavelId": 2, "prazoHoras": 48,
         "emissaoAutomatica": False, "riscoBase": 3}),
    req("Excluir", "DELETE", "/categorias/{{categoriaId}}", "tokenGestor"),
    req("Executor sugerido", "GET", "/categorias/4/executor-sugerido", "tokenGestor"),
    req("Erro 400 (validação)", "POST", "/categorias", "tokenGestor",
        {"nome": "", "icone": "x", "prazoHoras": 0, "emissaoAutomatica": True, "riscoBase": 9}),
    req("Erro 403 (cidadão não cria)", "POST", "/categorias", "tokenCidadao",
        {"nome": "X", "icone": "x", "executorResponsavelId": 1, "prazoHoras": 1, "emissaoAutomatica": True,
         "riscoBase": 1}),
]
executores = [
    req("Listar (paginado, filtro por nome)", "GET", "/executores?nome=equipe&page=0&size=10", "tokenSupervisor"),
    req("Criar", "POST", "/executores", "tokenGestor",
        {"nome": "Poda Verde Serviços", "tipo": "EMPRESA", "regiao": "Zona Sul",
         "contrato": "Contrato 045/2026 — Poda de árvores"}, testes=guardar("executorId")),
    req("Atualizar", "PUT", "/executores/{{executorId}}", "tokenGestor",
        {"nome": "Poda Verde Serviços", "tipo": "CONCESSIONARIA", "regiao": "Zona Sul",
         "contrato": "Contrato 045/2026 — Poda de árvores"}),
    req("Excluir", "DELETE", "/executores/{{executorId}}", "tokenGestor"),
    req("Erro 409 (executor com histórico)", "DELETE", "/executores/1", "tokenGestor"),
]
ocorrencias = [
    req("Triagem da foto por IA (multipart)", "POST", "/ocorrencias/triagem", "tokenCidadao", form=True,
        descricao="Escolha uma foto no campo 'arquivo'. Devolve fotoUrl, categoria, descrição e urgência sugeridas.",
        testes=guardar("fotoAntesUrl", "fotoUrl")),
    req("Reportar", "POST", "/ocorrencias", "tokenCidadao",
        {"categoriaId": 1, "descricao": "Poste apagado no meio da quadra.", "endereco": "R. Domingos de Morais, 1500",
         "latitude": -23.5889, "longitude": -46.6349, "fotoAntesUrl": "{{fotoAntesUrl}}", "urgente": False},
        testes=guardar("ocorrenciaId")),
    req("Reportar urgente (vai ao plantão)", "POST", "/ocorrencias", "tokenCidadao",
        {"categoriaId": 5, "descricao": "Árvore caída sobre a fiação.", "endereco": "R. Joaquim Távora, 300",
         "latitude": -23.5899, "longitude": -46.6341, "urgente": True}, testes=guardar("osUrgenteId", "osId")),
    req("Perto de mim", "GET", "/ocorrencias?lat=-23.5893&lng=-46.6347&raio=500", "tokenCidadao"),
    req("Minhas", "GET", "/ocorrencias/minhas", "tokenCidadao"),
    req("Detalhe", "GET", "/ocorrencias/{{ocorrenciaId}}", "tokenCidadao"),
    req("Editar (autor, antes da OS)", "PUT", "/ocorrencias/{{ocorrenciaId}}", "tokenCidadao",
        {"categoriaId": 1, "descricao": "Poste apagado e com a fiação exposta.", "endereco": "R. Domingos de Morais, 1500"}),
    req("Para confirmar", "GET", "/ocorrencias/para-confirmar", "tokenCidadao"),
    req("Confirmar (ainda existe)", "POST", "/ocorrencias/7/confirmacoes", "tokenCidadao", {"aindaExiste": True}),
    req("Excluir (autor, antes da OS)", "DELETE", "/ocorrencias/{{ocorrenciaId}}", "tokenCidadao"),
    req("Exceções (gestor)", "GET", "/ocorrencias/excecoes", "tokenGestor"),
    req("Emitir OS (gestor)", "POST", "/ocorrencias/6/emitir-os", "tokenGestor", {"executorId": 4, "prazoHoras": 120}),
    req("Encaminhar a outro órgão", "POST", "/ocorrencias/8/encaminhar", "tokenGestor",
        {"orgao": "DER-SP", "protocolo": "DER-2026-005120"}),
    req("Descartar", "POST", "/ocorrencias/7/descartar", "tokenGestor", {"justificativa": "Duplicada da ocorrência 1"}),
]
ordens = [
    req("Minhas OS (executor)", "GET", "/ordens/minhas", "tokenExecutor"),
    req("Aceitar", "POST", "/ordens/1/aceitar", "tokenExecutor"),
    req("Check-in longe (422)", "POST", "/ordens/1/checkin", "tokenExecutor", {"latitude": -23.58, "longitude": -46.6352}),
    req("Check-in no local", "POST", "/ordens/1/checkin", "tokenExecutor", {"latitude": -23.5885, "longitude": -46.6352}),
    req("Enviar foto do depois (multipart)", "POST", "/fotos", "tokenExecutor", form=True,
        testes=guardar("fotoDepoisUrl", "url")),
    req("Concluir", "POST", "/ordens/1/concluir", "tokenExecutor",
        {"fotoDepoisUrl": "{{fotoDepoisUrl}}", "observacao": "Lâmpada trocada."}),
    req("Reprovar (gestor, vira retrabalho)", "POST", "/ordens/1/reprovar", "tokenGestor",
        {"justificativa": "Ainda apaga à noite"}),
    req("Check-in de novo (retrabalho)", "POST", "/ordens/1/checkin", "tokenExecutor",
        {"latitude": -23.5885, "longitude": -46.6352}),
    req("Concluir de novo", "POST", "/ordens/1/concluir", "tokenExecutor",
        {"fotoDepoisUrl": "{{fotoDepoisUrl}}", "observacao": "Lâmpada e reator trocados."}),
    req("Aprovar (gestor)", "POST", "/ordens/1/aprovar", "tokenGestor"),
    req("Detalhe com timeline", "GET", "/ordens/1", "tokenGestor"),
    req("Confirmar solução (cidadão)", "POST", "/ordens/5/avaliacao", "tokenCidadao",
        {"aprovada": True, "nota": 5, "comentario": "Ficou ótimo"}),
    req("Painel com filtros", "GET", "/ordens?status=ACEITA&atrasadas=false&page=0&size=20", "tokenGestor"),
    req("Alterar prazo", "PATCH", "/ordens/2/prazo", "tokenGestor", {"horas": 12}),
    req("Cancelar", "POST", "/ordens/2/cancelar", "tokenGestor", {"justificativa": "Duplicada"}),
    req("Escalonadas (supervisor)", "GET", "/ordens/escalonadas", "tokenSupervisor"),
    req("Cobrar executor", "POST", "/ordens/4/cobrar", "tokenSupervisor"),
    req("Reatribuir", "POST", "/ordens/4/reatribuir", "tokenSupervisor", {"executorId": 3}),
]
outros = [
    req("Minhas notificações", "GET", "/notificacoes", "tokenCidadao"),
    req("Não lidas", "GET", "/notificacoes/nao-lidas", "tokenSupervisor"),
    req("Marcar todas como lidas", "POST", "/notificacoes/lidas", "tokenCidadao"),
    req("Indicadores (painel)", "GET", "/indicadores", "tokenGestor"),
    req("Indicadores por executor", "GET", "/indicadores/executores", "tokenSupervisor"),
    req("Indicadores por bairro", "GET", "/indicadores/bairros", "tokenGestor"),
    req("Executar verificação de prazos", "POST", "/prazos/executar", "tokenSupervisor"),
    req("Info da API", "GET", "/info"),
    req("Health check", "GET", "/actuator/health"),
]

colecao = {
    "info": {
        "name": "Zela API",
        "description": "Zeladoria colaborativa com IA (GS 2026/2). Rode primeiro a pasta '1. Autenticação' "
                       "para guardar os tokens de cada papel. Senha de todos os usuários de teste: 123456.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}]
                + [{"key": v, "value": ""} for _, v in PAPEIS.values()]
                + [{"key": k, "value": ""} for k in ["categoriaId", "executorId", "ocorrenciaId", "osUrgenteId"]]
                # Valores usados se você pular os uploads (a comparação da IA só roda com fotos de verdade).
                + [{"key": "fotoAntesUrl", "value": ""}, {"key": "fotoDepoisUrl", "value": "/fotos/exemplo-depois.jpg"}],
    "item": [
        pasta("1. Autenticação", login),
        pasta("2. Categorias (CRUD)", categorias),
        pasta("3. Executores (CRUD)", executores),
        pasta("4. Ocorrências e IA", ocorrencias),
        pasta("5. Ordens de serviço", ordens),
        pasta("6. Notificações, indicadores e prazos", outros),
    ],
}

destino = Path("postman/zela-api.postman_collection.json")
destino.parent.mkdir(exist_ok=True)
destino.write_text(json.dumps(colecao, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
total = sum(len(p["item"]) for p in colecao["item"])
print(f"{destino}: {total} requisições")
