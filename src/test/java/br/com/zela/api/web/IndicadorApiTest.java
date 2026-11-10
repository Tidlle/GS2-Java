package br.com.zela.api.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Indicadores sobre a carga inicial: 9 OS, das quais 7 em etapas abertas (1 atrasada e escalada)
 * e 3 já concluídas (OS 5, 6 e 8, todas dentro do prazo; a 6 teve retrabalho).
 */
class IndicadorApiTest extends TesteDeApi {

    @Test
    void painelGeral() throws Exception {
        mvc.perform(como(GESTOR, get("/indicadores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertas").value(7))
                .andExpect(jsonPath("$.atrasadas").value(1))
                .andExpect(jsonPath("$.escaladas").value(1))
                .andExpect(jsonPath("$.concluidas").value(3))
                .andExpect(jsonPath("$.prazosCumpridosPct").value(100))
                .andExpect(jsonPath("$.retrabalhoPct").value(33))
                .andExpect(jsonPath("$.tempoMedioHoras").isNumber())
                .andExpect(jsonPath("$.excecoes").value(1))
                .andExpect(jsonPath("$.bairros", hasSize(1)))
                .andExpect(jsonPath("$.bairros[0].bairro").value("Vila Mariana"))
                .andExpect(jsonPath("$.bairros[0].ocorrencias").value(13))
                .andExpect(jsonPath("$.bairros[0].resolvidas").value(3));
    }

    @Test
    void desempenhoPorExecutor() throws Exception {
        mvc.perform(como(SUPERVISOR, get("/indicadores/executores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.nome == 'Equipe Luz Norte')].totalOS").value(3))
                .andExpect(jsonPath("$[?(@.nome == 'Equipe Luz Norte')].concluidas").value(1))
                .andExpect(jsonPath("$[?(@.nome == 'Calçadas & Cia')].retrabalhoPct").value(100))
                .andExpect(jsonPath("$[?(@.nome == 'Defesa Civil / Parques')].atrasadas").value(1));
    }

    @Test
    void avaliacaoDoCidadaoEntraNaMediaDoExecutor() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ordens/5/avaliacao")), "{\"aprovada\": true, \"nota\": 4}"))
                .andExpect(status().isOk());
        mvc.perform(como(GESTOR, get("/indicadores/executores")))
                .andExpect(jsonPath("$[?(@.nome == 'Equipe Luz Norte')].avaliacao").value(4.0))
                .andExpect(jsonPath("$[?(@.nome == 'Equipe Luz Norte')].avaliacoes").value(1));
    }

    @Test
    void bairrosESoParaCoordenacao() throws Exception {
        mvc.perform(como(GESTOR, get("/indicadores/bairros"))).andExpect(jsonPath("$[0].bairro").value("Vila Mariana"));
        mvc.perform(como(CIDADAO, get("/indicadores"))).andExpect(status().isForbidden());
        mvc.perform(como(EXECUTOR, get("/indicadores/executores"))).andExpect(status().isForbidden());
    }
}
