package br.com.zela.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.repositorio.AvaliacaoRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Carga inicial usada: OS 1 (EMITIDA), 5 (CONCLUIDA) e 9 (ACEITA) são da Equipe Luz Norte (executor@zela.com);
 * OS 4 está ESCALADA; OS 6 está REABERTA. As ocorrências 1, 4, 5 e 11 são da Ana (cidadao@zela.com).
 */
class OrdemServicoApiTest extends TesteDeApi {

    private static final String PAVIMENTACAO = "pavimentacao@zela.com";
    private static final String CIDADAO2 = "cidadao2@zela.com";
    private static final String NO_LOCAL_DA_OS_1 = "{\"latitude\": -23.5885, \"longitude\": -46.6352}";

    @Autowired OrdemServicoRepository ordens;
    @Autowired AvaliacaoRepository avaliacoes;

    // ---------- Consultas e posse ----------

    @Test
    void executorVeSoAsOSDaSuaEquipe() throws Exception {
        mvc.perform(como(EXECUTOR, get("/ordens/minhas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].executorNome", hasItem("Equipe Luz Norte")))
                .andExpect(jsonPath("$.content[0].ocorrencia.titulo").exists())
                .andExpect(jsonPath("$.content[0].timeline").doesNotExist());

        mvc.perform(como(EXECUTOR, get("/ordens/minhas").param("status", "ACEITA")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(9));

        mvc.perform(como(EXECUTOR, get("/ordens/2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Esta OS é de outro executor"));
    }

    @Test
    void cidadaoVeSoAsOSDasSuasOcorrencias() throws Exception {
        mvc.perform(como(CIDADAO, get("/ordens/5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline", hasSize(4)))
                .andExpect(jsonPath("$.timeline[3].usuario").value("Equipe Luz Norte"))
                .andExpect(jsonPath("$.confiancaIA").value(0.93));
        mvc.perform(como(CIDADAO2, get("/ordens/5"))).andExpect(status().isForbidden());
        mvc.perform(como(CIDADAO, get("/ordens"))).andExpect(status().isForbidden());
    }

    @Test
    void painelDoGestorFiltra() throws Exception {
        mvc.perform(como(GESTOR, get("/ordens"))).andExpect(jsonPath("$.page.totalElements").value(9));
        mvc.perform(como(GESTOR, get("/ordens").param("status", "REABERTA")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].retrabalho").value(true));
        mvc.perform(como(GESTOR, get("/ordens").param("executorId", "1")))
                .andExpect(jsonPath("$.page.totalElements").value(3));
        mvc.perform(como(SUPERVISOR, get("/ordens").param("atrasadas", "true")))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.content[0].atrasada").value(true));
        mvc.perform(como(GESTOR, get("/ordens").param("bairro", "vila")))
                .andExpect(jsonPath("$.page.totalElements").value(9));
    }

    // ---------- Fluxo completo ----------

    @Test
    void cicloCompletoDoExecutorAteAAprovacao() throws Exception {
        mvc.perform(como(EXECUTOR, post("/ordens/1/aceitar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));

        mvc.perform(json(como(EXECUTOR, post("/ordens/1/checkin")), "{\"latitude\": -23.5800, \"longitude\": -46.6352}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Você está a 934 m do local; o check-in exige no máximo 100 m"));

        mvc.perform(json(como(EXECUTOR, post("/ordens/1/checkin")), NO_LOCAL_DA_OS_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.checkinLat").value(-23.5885))
                .andExpect(jsonPath("$.timeline[2].justificativa").value("Check-in a 11 m do local"));

        mvc.perform(json(como(EXECUTOR, post("/ordens/1/concluir")), "{\"observacao\": \"Sem foto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("A foto do depois é obrigatória"));

        mvc.perform(json(como(EXECUTOR, post("/ordens/1/concluir")),
                        "{\"fotoDepoisUrl\": \"/fotos/depois-1.jpg\", \"observacao\": \"Lâmpada trocada.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluidaEm").exists());

        mvc.perform(como(GESTOR, post("/ordens/1/aprovar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                // 6 mudanças de status + 1 registro da IA (sem foto do antes: comparação indisponível).
                .andExpect(jsonPath("$.timeline", hasSize(7)))
                .andExpect(jsonPath("$.timeline[4].usuario").value("IA (simulada)"))
                .andExpect(jsonPath("$.timeline[5].statusNovo").value("VALIDADA"))
                .andExpect(jsonPath("$.timeline[5].usuario").value("Carlos Lima"));

        mvc.perform(como(CIDADAO, get("/ocorrencias/1"))).andExpect(jsonPath("$.status").value("RESOLVIDA"));
    }

    @Test
    void naoPulaEtapasNemAceitaDuasVezes() throws Exception {
        mvc.perform(json(como(EXECUTOR, post("/ordens/1/concluir")), "{\"fotoDepoisUrl\": \"/fotos/x.jpg\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("OS não pode passar de EMITIDA para CONCLUIDA"));
        mvc.perform(como(EXECUTOR, post("/ordens/1/aceitar"))).andExpect(status().isOk());
        mvc.perform(como(EXECUTOR, post("/ordens/1/aceitar"))).andExpect(status().isConflict());
    }

    @Test
    void executorDeOutraEquipeNaoMexe() throws Exception {
        mvc.perform(como(PAVIMENTACAO, post("/ordens/1/aceitar"))).andExpect(status().isForbidden());
        mvc.perform(como(GESTOR, post("/ordens/1/aceitar"))).andExpect(status().isForbidden());
    }

    // ---------- Recusa e encaminhamento ----------

    @Test
    void recusaComMotivoEGestorEncaminha() throws Exception {
        mvc.perform(json(como(EXECUTOR, post("/ordens/1/recusar")), "{\"justificativa\": \"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(como(EXECUTOR, post("/ordens/1/recusar")),
                        "{\"justificativa\": \"Poste da concessionária, fora do contrato\"}"))
                .andExpect(jsonPath("$.status").value("RECUSADA"));

        mvc.perform(json(como(GESTOR, post("/ordens/1/encaminhar")), "{\"orgao\": \"Enel\", \"protocolo\": \"ENEL-55120\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCAMINHADA"))
                .andExpect(jsonPath("$.protocoloExterno").value("ENEL-55120"));
        mvc.perform(como(CIDADAO, get("/ocorrencias/1")))
                .andExpect(jsonPath("$.status").value("ENCAMINHADA"))
                .andExpect(jsonPath("$.orgaoEncaminhado").value("Enel"));
    }

    // ---------- Gestor ----------

    @Test
    void reprovacaoReabreComoRetrabalho() throws Exception {
        mvc.perform(json(como(GESTOR, post("/ordens/5/reprovar")), "{\"justificativa\": \"Ainda apaga à noite\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REABERTA"))
                .andExpect(jsonPath("$.retrabalho").value(true));
        OrdemServico os = ordens.findById(5L).orElseThrow();
        assertThat(Duration.between(Instant.now(), os.getPrazoEtapa()).toHours()).isEqualTo(23);
    }

    @Test
    void gestorAlteraPrazoECancela() throws Exception {
        mvc.perform(json(como(GESTOR, patch("/ordens/1/prazo")), "{\"horas\": 0}")).andExpect(status().isBadRequest());
        mvc.perform(json(como(GESTOR, patch("/ordens/1/prazo")), "{\"horas\": 12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline[1].justificativa").value("Prazo da etapa alterado para 12 h"))
                .andExpect(jsonPath("$.timeline[1].statusAnterior").doesNotExist());

        mvc.perform(json(como(GESTOR, post("/ordens/1/cancelar")), "{\"justificativa\": \"Duplicada\"}"))
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.timeline[2].justificativa").value("Cancelada: Duplicada"));
        mvc.perform(como(CIDADAO, get("/ocorrencias/1")))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.osId").doesNotExist());
        // Volta para a fila de exceções e pode receber nova OS.
        mvc.perform(como(GESTOR, get("/ocorrencias/excecoes"))).andExpect(jsonPath("$.content[*].id", hasItem(1)));
        mvc.perform(como(GESTOR, post("/ocorrencias/1/emitir-os"))).andExpect(status().isOk());
    }

    // ---------- Cidadão ----------

    @Test
    void cidadaoConfirmaASolucao() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ordens/5/avaliacao")),
                        "{\"aprovada\": true, \"nota\": 5, \"comentario\": \"Ficou ótimo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.cidadaoConfirmou").value(true));
        mvc.perform(como(CIDADAO, get("/ocorrencias/11"))).andExpect(jsonPath("$.status").value("RESOLVIDA"));
        assertThat(avaliacoes.findByOrdemExecutorId(1L)).singleElement()
                .satisfies(a -> assertThat(a.getNota()).isEqualTo(5));
    }

    @Test
    void cidadaoContestaEAOSVoltaAoExecutor() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ordens/5/avaliacao")),
                        "{\"aprovada\": false, \"comentario\": \"Continua apagado\"}"))
                .andExpect(jsonPath("$.status").value("REABERTA"))
                .andExpect(jsonPath("$.retrabalho").value(true))
                .andExpect(jsonPath("$.timeline[4].justificativa").value("Continua apagado"));
        // Retrabalho: o executor volta ao local.
        mvc.perform(json(como(EXECUTOR, post("/ordens/5/checkin")), "{\"latitude\": -23.5880, \"longitude\": -46.6350}"))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void soQuemReportouAvaliaESoOSConcluida() throws Exception {
        mvc.perform(json(como(CIDADAO2, post("/ordens/5/avaliacao")), "{\"aprovada\": true}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(como(CIDADAO, post("/ordens/1/avaliacao")), "{\"aprovada\": true}"))
                .andExpect(status().isConflict());
        mvc.perform(json(como(CIDADAO, post("/ordens/5/avaliacao")), "{\"aprovada\": true, \"nota\": 7}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Supervisor ----------

    @Test
    void supervisorCobraEReatribuiOSEscalada() throws Exception {
        mvc.perform(como(SUPERVISOR, get("/ordens/escalonadas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(4));

        mvc.perform(como(SUPERVISOR, post("/ordens/4/cobrar")))
                .andExpect(jsonPath("$.timeline[2].justificativa").value("Supervisor cobrou o executor"))
                .andExpect(jsonPath("$.timeline[2].usuario").value("Marina Costa"));

        mvc.perform(json(como(SUPERVISOR, post("/ordens/4/reatribuir")), "{\"executorId\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMITIDA"))
                .andExpect(jsonPath("$.executorNome").value("Equipe Limpeza Urbana (própria)"))
                .andExpect(jsonPath("$.atrasada").value(false))
                .andExpect(jsonPath("$.timeline[3].justificativa").value(
                        "Reatribuída de Defesa Civil / Parques para Equipe Limpeza Urbana (própria)"));
        // Urgente: o novo prazo é o de plantão (4 h).
        OrdemServico os = ordens.findById(4L).orElseThrow();
        assertThat(Duration.between(Instant.now(), os.getPrazoEtapa()).toHours()).isEqualTo(3);

        mvc.perform(como(SUPERVISOR, get("/ordens/escalonadas"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void soSupervisorCobra() throws Exception {
        mvc.perform(como(CIDADAO, post("/ordens/4/cobrar"))).andExpect(status().isForbidden());
        mvc.perform(como(EXECUTOR, get("/ordens/escalonadas"))).andExpect(status().isForbidden());
    }

    @Test
    void osInexistenteDa404() throws Exception {
        mvc.perform(como(GESTOR, get("/ordens/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("OS 999 não encontrado(a)"));
    }
}
