package br.com.zela.api.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class ExecutorApiTest extends TesteDeApi {

    private static final String NOVO = """
            {"nome": "Poda Verde Serviços", "tipo": "EMPRESA", "regiao": "Zona Sul",
             "contrato": "Contrato 045/2026 — Poda de árvores"}""";

    @Test
    void supervisorListaEFiltraPorNome() throws Exception {
        mvc.perform(como(SUPERVISOR, get("/executores").param("nome", "equipe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].nome").value("Equipe Limpeza Urbana (própria)"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void cidadaoEExecutorNaoVeemExecutores() throws Exception {
        mvc.perform(como(CIDADAO, get("/executores"))).andExpect(status().isForbidden());
        mvc.perform(como(EXECUTOR, get("/executores"))).andExpect(status().isForbidden());
    }

    @Test
    void supervisorNaoAlteraExecutor() throws Exception {
        mvc.perform(json(como(SUPERVISOR, post("/executores")), NOVO)).andExpect(status().isForbidden());
    }

    @Test
    void gestorFazOCrudCompleto() throws Exception {
        String criado = mvc.perform(json(como(GESTOR, post("/executores")), NOVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("EMPRESA"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");

        mvc.perform(json(como(GESTOR, put("/executores/" + id)), NOVO.replace("EMPRESA", "CONCESSIONARIA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("CONCESSIONARIA"));

        mvc.perform(como(GESTOR, delete("/executores/" + id))).andExpect(status().isNoContent());
        mvc.perform(como(GESTOR, get("/executores/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void tipoInvalidoDa400() throws Exception {
        mvc.perform(json(como(GESTOR, post("/executores")), NOVO.replace("EMPRESA", "ONG")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executorComHistoricoNaoPodeSerExcluido() throws Exception {
        mvc.perform(como(GESTOR, delete("/executores/1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Executor é responsável por categorias; troque o responsável antes de excluir"));
    }
}
