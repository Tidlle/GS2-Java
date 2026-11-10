package br.com.zela.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.zela.api.repositorio.OrdemServicoRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de prazo disparadas por POST /prazos/executar (o mesmo ciclo do agendador).
 * O "relógio" é simulado voltando datas no banco. Cada item é gravado na própria transação,
 * então o teste roda sem rollback, num banco H2 só dele; cada caso usa registros diferentes.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:zela-prazos;DB_CLOSE_DELAY=-1;TIME ZONE=UTC")
class PrazoApiTest extends TesteDeApi {

    @Autowired JdbcTemplate jdbc;
    @Autowired OrdemServicoRepository ordens;

    private void executarCiclo() throws Exception {
        mvc.perform(como(SUPERVISOR, post("/prazos/executar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executadoEm").exists())
                .andExpect(jsonPath("$.falhas").value(0));
    }

    @Test
    void etapaComPrazoVencidoEhEscaladaEAvisaOSupervisor() throws Exception {
        jdbc.update("update ordens_servico set prazo_etapa = DATEADD(HOUR, -2, LOCALTIMESTAMP) where id = 1");

        executarCiclo();

        mvc.perform(como(GESTOR, get("/ordens/1")))
                .andExpect(jsonPath("$.status").value("ESCALADA"))
                .andExpect(jsonPath("$.timeline[-1:].usuario", hasItem("Sistema (prazos)")))
                .andExpect(jsonPath("$.timeline[-1:].justificativa", hasItem("Prazo da etapa EMITIDA estourado")));
        mvc.perform(como(SUPERVISOR, get("/notificacoes").param("size", "50")))
                .andExpect(jsonPath("$.content[*].texto", hasItem(startsWith("Prazo estourado: OS #1 "))));

        // O executor retoma: a nova etapa ganha prazo de recuperação e não é escalada de novo.
        mvc.perform(como(EXECUTOR, post("/ordens/1/aceitar"))).andExpect(jsonPath("$.status").value("ACEITA"));
        assertThat(ordens.findById(1L).orElseThrow().getPrazoEtapa())
                .isAfter(Instant.now().plus(Duration.ofHours(23)));
        executarCiclo();
        mvc.perform(como(GESTOR, get("/ordens/1"))).andExpect(jsonPath("$.status").value("ACEITA"));
    }

    @Test
    void concluidaComIaConfianteESemContestacaoEm48hTemAceiteTacito() throws Exception {
        jdbc.update("update ordens_servico set concluida_em = DATEADD(HOUR, -50, LOCALTIMESTAMP) where id = 5");

        executarCiclo();

        mvc.perform(como(GESTOR, get("/ordens/5")))
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.timeline[*].usuario", hasItem("Aceite tácito (IA 93%, sem contestação em 48 h)")));
        mvc.perform(como(CIDADAO, get("/ocorrencias/11"))).andExpect(jsonPath("$.status").value("RESOLVIDA"));
    }

    @Test
    void confiancaBaixaNaoTemAceiteTacito() throws Exception {
        jdbc.update("""
                update ordens_servico set status = 'CONCLUIDA', confianca_ia = 0.5,
                       concluida_em = DATEADD(HOUR, -60, LOCALTIMESTAMP) where id = 9""");

        executarCiclo();

        mvc.perform(como(GESTOR, get("/ordens/9"))).andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void semRespostaDosVizinhosEm24hAOcorrenciaSegue() throws Exception {
        jdbc.update("update ocorrencias set criada_em = DATEADD(HOUR, -25, LOCALTIMESTAMP) where id = 7");

        executarCiclo();

        // Iluminação tem emissão automática: seguiu e já virou OS.
        mvc.perform(como(CIDADAO, get("/ocorrencias/7")))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.osId").isNumber());
        mvc.perform(como("cidadao2@zela.com", get("/notificacoes").param("size", "50")))
                .andExpect(jsonPath("$.content[*].texto", hasItem(
                        "Sua ocorrência \"Lâmpada piscando na Praça da Árvore\" seguiu para atendimento: "
                                + "sem resposta dos vizinhos em 24 h.")));
    }

    @Test
    void soCoordenacaoDisparaOCiclo() throws Exception {
        mvc.perform(como(CIDADAO, post("/prazos/executar"))).andExpect(status().isForbidden());
        mvc.perform(como(EXECUTOR, post("/prazos/executar"))).andExpect(status().isForbidden());
    }
}
