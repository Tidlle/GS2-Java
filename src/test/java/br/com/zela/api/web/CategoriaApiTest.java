package br.com.zela.api.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

class CategoriaApiTest extends TesteDeApi {

    private static final String NOVA = """
            {"nome": "Sinalização", "icone": "warning-outline", "executorResponsavelId": 2,
             "prazoHoras": 72, "emissaoAutomatica": true, "riscoBase": 2}""";

    @Test
    void qualquerUsuarioLogadoListaPaginado() throws Exception {
        // Ordenação por id: a ordem alfabética de nomes acentuados depende do banco.
        mvc.perform(como(CIDADAO, get("/categorias").param("size", "2").param("sort", "id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].nome").value("Iluminação"))
                .andExpect(jsonPath("$.content[0].executorResponsavelNome").value("Equipe Luz Norte"))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.page.size").value(2));
    }

    @Test
    void cidadaoNaoCriaCategoria() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/categorias")), NOVA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void gestorFazOCrudCompleto() throws Exception {
        String criada = mvc.perform(json(como(GESTOR, post("/categorias")), NOVA))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Sinalização"))
                .andExpect(jsonPath("$.executorResponsavelNome").value("Via Segura Pavimentação"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criada, "$.id");

        mvc.perform(como(CIDADAO, get("/categorias/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prazoHoras").value(72));

        mvc.perform(json(como(GESTOR, put("/categorias/" + id)), """
                        {"nome": "Sinalização viária", "icone": "warning-outline", "executorResponsavelId": 2,
                         "prazoHoras": 48, "emissaoAutomatica": false, "riscoBase": 3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sinalização viária"))
                .andExpect(jsonPath("$.emissaoAutomatica").value(false));

        mvc.perform(como(GESTOR, delete("/categorias/" + id)))
                .andExpect(status().isNoContent());
        mvc.perform(como(GESTOR, get("/categorias/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Categoria " + id + " não encontrado(a)"));
    }

    @Test
    void validaCamposDaCategoria() throws Exception {
        mvc.perform(json(como(GESTOR, post("/categorias")), """
                        {"nome": "", "icone": "x", "executorResponsavelId": null, "prazoHoras": 0,
                         "emissaoAutomatica": true, "riscoBase": 9}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(4)))
                .andExpect(jsonPath("$.erros[?(@.campo == 'riscoBase')].mensagem").value("O risco base vai de 1 a 5"));
    }

    @Test
    void nomeRepetidoDa409() throws Exception {
        mvc.perform(json(como(GESTOR, post("/categorias")), NOVA.replace("Sinalização", "iluminação")))
                .andExpect(status().isConflict());
    }

    @Test
    void executorInexistenteDa404() throws Exception {
        mvc.perform(json(como(GESTOR, post("/categorias")), NOVA.replace("\"executorResponsavelId\": 2", "\"executorResponsavelId\": 999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Executor 999 não encontrado(a)"));
    }

    @Test
    void categoriaUsadaEmOcorrenciasNaoPodeSerExcluida() throws Exception {
        mvc.perform(como(GESTOR, delete("/categorias/1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Categoria já usada em ocorrências e não pode ser excluída"));
    }

    @Test
    void idComTipoErradoDa400() throws Exception {
        mvc.perform(como(CIDADAO, get("/categorias/abc")))
                .andExpect(status().isBadRequest());
    }
}
