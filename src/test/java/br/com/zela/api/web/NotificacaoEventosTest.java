package br.com.zela.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eventos → notificações. Os avisos só são gerados DEPOIS do commit, então este teste não usa rollback
 * e roda num banco H2 próprio (para não alterar os dados vistos pelos outros testes).
 * Mensageria no modo local: o mesmo processador que os consumidores do RabbitMQ usam.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:zela-eventos;DB_CLOSE_DELAY=-1;TIME ZONE=UTC")
class NotificacaoEventosTest extends TesteDeApi {

    private static final String PLANTAO = "plantao@zela.com";

    private long naoLidas(String email) throws Exception {
        String r = mvc.perform(como(email, get("/notificacoes/nao-lidas")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(r, "$.naoLidas")).longValue();
    }

    private Integer reportarUrgente(long categoriaId) throws Exception {
        String r = mvc.perform(json(como(CIDADAO, post("/ocorrencias")), """
                        {"categoriaId": %d, "descricao": "Risco na via.", "latitude": -23.5891,
                         "longitude": -46.6348, "urgente": true}""".formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.osId");
    }

    @Test
    void urgenciaAvisaCoordenacaoCidadaoEPlantao() throws Exception {
        long supervisorAntes = naoLidas(SUPERVISOR);
        long gestorAntes = naoLidas(GESTOR);
        long plantaoAntes = naoLidas(PLANTAO);

        Integer osId = reportarUrgente(5); // Árvore → Defesa Civil / Parques (plantao@zela.com)

        assertThat(naoLidas(SUPERVISOR)).isEqualTo(supervisorAntes + 1);
        assertThat(naoLidas(GESTOR)).isEqualTo(gestorAntes + 1);
        assertThat(naoLidas(PLANTAO)).isEqualTo(plantaoAntes + 1);
        mvc.perform(como(SUPERVISOR, get("/notificacoes")))
                .andExpect(jsonPath("$.content[0].texto", startsWith("URGENTE:")))
                .andExpect(jsonPath("$.content[0].lida").value(false));
        mvc.perform(como(PLANTAO, get("/notificacoes")))
                .andExpect(jsonPath("$.content[0].texto").value("Nova OS #%d: Árvore: Localização por GPS.".formatted(osId)));
        mvc.perform(como(CIDADAO, get("/notificacoes")))
                .andExpect(jsonPath("$.content[*].texto", hasItem(startsWith("Sua ocorrência virou OS #" + osId))));
    }

    @Test
    void cidadaoAcompanhaOServicoPelosAvisos() throws Exception {
        Integer osId = reportarUrgente(1); // Iluminação → Equipe Luz Norte (executor@zela.com)
        String os = "/ordens/" + osId;

        mvc.perform(como(EXECUTOR, post(os + "/aceitar"))).andExpect(status().isOk());
        mvc.perform(json(como(EXECUTOR, post(os + "/checkin")), "{\"latitude\": -23.5891, \"longitude\": -46.6348}"))
                .andExpect(status().isOk());
        mvc.perform(json(como(EXECUTOR, post(os + "/concluir")), "{\"fotoDepoisUrl\": \"/fotos/x.jpg\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(como(CIDADAO, post(os + "/avaliacao")), "{\"aprovada\": true, \"nota\": 5}"))
                .andExpect(status().isOk());

        mvc.perform(como(CIDADAO, get("/notificacoes").param("size", "50")))
                .andExpect(jsonPath("$.content[*].texto", hasItem(startsWith("A equipe chegou ao local"))))
                .andExpect(jsonPath("$.content[*].texto", hasItem(
                        "O problema \"Iluminação: Localização por GPS\" foi concluído. Confirme a solução.")))
                .andExpect(jsonPath("$.content[*].texto", hasItem(startsWith("Sua ocorrência \"Iluminação"))));
    }

    @Test
    void operacaoRecusadaNaoGeraAviso() throws Exception {
        Integer osId = reportarUrgente(1);
        mvc.perform(como(EXECUTOR, post("/ordens/" + osId + "/aceitar"))).andExpect(status().isOk());
        long antes = naoLidas(CIDADAO);
        // Transição inválida: dá 409, a transação é desfeita e nada é publicado.
        mvc.perform(json(como(EXECUTOR, post("/ordens/" + osId + "/concluir")), "{\"fotoDepoisUrl\": \"/fotos/x.jpg\"}"))
                .andExpect(status().isConflict());
        assertThat(naoLidas(CIDADAO)).isEqualTo(antes);
    }

    @Test
    void marcaTodasComoLidas() throws Exception {
        reportarUrgente(5);
        assertThat(naoLidas(SUPERVISOR)).isPositive();
        mvc.perform(como(SUPERVISOR, post("/notificacoes/lidas"))).andExpect(status().isNoContent());
        assertThat(naoLidas(SUPERVISOR)).isZero();
        mvc.perform(como(SUPERVISOR, get("/notificacoes"))).andExpect(jsonPath("$.content[0].lida").value(true));
    }

    @Test
    void notificacoesExigemLogin() throws Exception {
        mvc.perform(get("/notificacoes")).andExpect(status().isUnauthorized());
    }
}
