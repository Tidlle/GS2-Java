package br.com.zela.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import br.com.zela.api.repositorio.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OcorrenciaApiTest extends TesteDeApi {

    private static final String CIDADAO2 = "cidadao2@zela.com";

    @Autowired OrdemServicoRepository ordens;
    @Autowired UsuarioRepository usuarios;

    private static String reporte(long categoriaId, boolean urgente) {
        return """
                {"categoriaId": %d, "descricao": "Poste apagado no meio da quadra.", "endereco": "R. Teste, 100",
                 "latitude": -23.5889, "longitude": -46.6349, "urgente": %s}""".formatted(categoriaId, urgente);
    }

    private Integer criar(String email, String corpo) throws Exception {
        String resposta = mvc.perform(json(como(email, post("/ocorrencias")), corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    /** Cria um cidadão novo pelo cadastro público (senha 123456) e devolve o e-mail. */
    private String novoCidadao(String nome) throws Exception {
        String email = nome.toLowerCase() + "@teste.com";
        mvc.perform(json(post("/auth/cadastro"), """
                        {"nome": "%s", "email": "%s", "senha": "123456", "bairro": "Vila Mariana"}"""
                        .formatted(nome, email)))
                .andExpect(status().isCreated());
        return email;
    }

    private void votar(String email, long ocorrenciaId, boolean aindaExiste) throws Exception {
        mvc.perform(json(como(email, post("/ocorrencias/" + ocorrenciaId + "/confirmacoes")),
                        "{\"aindaExiste\": %s}".formatted(aindaExiste)))
                .andExpect(status().isOk());
    }

    // ---------- Reporte ----------

    @Test
    void reporteNormalAguardaConfirmacao() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ocorrencias")), reporte(1, false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                .andExpect(jsonPath("$.titulo").value("Iluminação: R. Teste, 100"))
                .andExpect(jsonPath("$.categoriaNome").value("Iluminação"))
                .andExpect(jsonPath("$.bairro").value("Vila Mariana"))
                .andExpect(jsonPath("$.prioridade").value(2))
                .andExpect(jsonPath("$.autorId").value(1))
                .andExpect(jsonPath("$.osId").doesNotExist());
    }

    @Test
    void reporteUrgentePulaConfirmacaoEVaiAoPlantao() throws Exception {
        String resposta = mvc.perform(json(como(CIDADAO, post("/ocorrencias")), reporte(5, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("URGENTE"))
                .andExpect(jsonPath("$.prioridade").value(5))
                .andExpect(jsonPath("$.osId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Integer osId = JsonPath.read(resposta, "$.osId");
        OrdemServico os = ordens.findById(osId.longValue()).orElseThrow();
        assertThat(os.getStatus()).isEqualTo(StatusOS.EMITIDA);
        assertThat(os.getExecutor().getNome()).isEqualTo("Defesa Civil / Parques");
        assertThat(Duration.between(Instant.now(), os.getPrazoEtapa()).toMinutes()).isCloseTo(240, within(2L));
        assertThat(os.getEventos().getFirst().getAutorNome()).isEqualTo("Sistema (urgência — plantão)");
    }

    @Test
    void semEnderecoUsaLocalizacaoPorGps() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ocorrencias")), """
                        {"categoriaId": 3, "descricao": "Entulho.", "latitude": -23.58, "longitude": -46.63}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endereco").value("Localização por GPS"))
                .andExpect(jsonPath("$.urgente").value(false));
    }

    @Test
    void sohCidadaoReporta() throws Exception {
        mvc.perform(json(como(GESTOR, post("/ocorrencias")), reporte(1, false))).andExpect(status().isForbidden());
    }

    @Test
    void validaCoordenadasEDescricao() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ocorrencias")), """
                        {"categoriaId": 1, "descricao": " ", "latitude": 200, "longitude": -46.6}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(2)))
                .andExpect(jsonPath("$.erros[0].campo").value("descricao"))
                .andExpect(jsonPath("$.erros[1].mensagem").value("Latitude entre -90 e 90"));
    }

    @Test
    void categoriaInexistenteDa404() throws Exception {
        mvc.perform(json(como(CIDADAO, post("/ocorrencias")), reporte(99, false))).andExpect(status().isNotFound());
    }

    // ---------- Consulta ----------

    @Test
    void listaPorProximidadeComDistancia() throws Exception {
        mvc.perform(como(CIDADAO, get("/ocorrencias")
                        .param("lat", "-23.5893").param("lng", "-46.6347").param("raio", "120")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].distanciaM").value(88))
                .andExpect(jsonPath("$.content[1].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void listaSemPosicaoPaginaPorData() throws Exception {
        mvc.perform(como(EXECUTOR, get("/ocorrencias").param("size", "5").param("status", "AGUARDANDO_CONFIRMACAO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(8))
                .andExpect(jsonPath("$.content[0].distanciaM").doesNotExist());
    }

    @Test
    void minhasTrazSoAsDoCidadaoLogado() throws Exception {
        mvc.perform(como(CIDADAO2, get("/ocorrencias/minhas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].autorId", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(5))));
    }

    @Test
    void detalheMostraOsVigenteEProtocolo() throws Exception {
        mvc.perform(como(CIDADAO, get("/ocorrencias/10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCAMINHADA"))
                .andExpect(jsonPath("$.protocoloExterno").value("DER-2026-004871"))
                .andExpect(jsonPath("$.osId").value(7));
    }

    // ---------- Edição e exclusão pelo autor ----------

    @Test
    void autorEditaEExcluiAntesDeVirarOS() throws Exception {
        Integer id = criar(CIDADAO, reporte(1, false));

        mvc.perform(json(como(CIDADAO, put("/ocorrencias/" + id)), """
                        {"categoriaId": 2, "descricao": "Na verdade é um buraco.", "endereco": "R. Teste, 120"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaNome").value("Buraco"))
                .andExpect(jsonPath("$.titulo").value("Buraco: R. Teste, 120"));

        mvc.perform(como(CIDADAO, delete("/ocorrencias/" + id))).andExpect(status().isNoContent());
        mvc.perform(como(CIDADAO, get("/ocorrencias/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void outroCidadaoNaoEditaNemExclui() throws Exception {
        Integer id = criar(CIDADAO, reporte(1, false));
        mvc.perform(json(como(CIDADAO2, put("/ocorrencias/" + id)), """
                        {"categoriaId": 1, "descricao": "Invasão"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Só quem reportou pode alterar esta ocorrência"));
        mvc.perform(como(CIDADAO2, delete("/ocorrencias/" + id))).andExpect(status().isForbidden());
    }

    @Test
    void ocorrenciaComOSNaoPodeSerAlterada() throws Exception {
        mvc.perform(como(CIDADAO, delete("/ocorrencias/1")))
                .andExpect(status().isConflict());
    }

    // ---------- Confirmação dos vizinhos ----------

    @Test
    void paraConfirmarMostraSoOcorrenciasDeOutrosAindaNaoRespondidas() throws Exception {
        mvc.perform(como(CIDADAO, get("/ocorrencias/para-confirmar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(8))
                .andExpect(jsonPath("$[1].id").value(7));
        // João é o autor das duas.
        mvc.perform(como(CIDADAO2, get("/ocorrencias/para-confirmar")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void terceiraConfirmacaoDeRotinaViraOSAutomatica() throws Exception {
        int reputacaoAntes = usuarios.findById(5L).orElseThrow().getReputacao();

        votar(CIDADAO, 7, true); // 2º "sim" (a carga já tem 1)
        mvc.perform(como(CIDADAO, get("/ocorrencias/7")))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CONFIRMACAO"))
                .andExpect(jsonPath("$.prioridade").value(3));

        mvc.perform(json(como(novoCidadao("Bruno"), post("/ocorrencias/7/confirmacoes")), "{\"aindaExiste\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmacoesSim").value(3))
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andExpect(jsonPath("$.osId", notNullValue()));

        assertThat(usuarios.findById(5L).orElseThrow().getReputacao()).isEqualTo(reputacaoAntes + 2);
    }

    @Test
    void tresNegacoesDescartamEReduzemReputacaoDoAutor() throws Exception {
        int reputacaoAntes = usuarios.findById(5L).orElseThrow().getReputacao();
        votar(CIDADAO, 8, false);
        votar(novoCidadao("Carla"), 8, false);
        mvc.perform(json(como(novoCidadao("Davi"), post("/ocorrencias/8/confirmacoes")), "{\"aindaExiste\": false}"))
                .andExpect(jsonPath("$.status").value("DESCARTADA"))
                .andExpect(jsonPath("$.motivoDescarte").value(
                        "Negada pelos vizinhos: 3 responderam que o problema não existe mais"));
        assertThat(usuarios.findById(5L).orElseThrow().getReputacao()).isEqualTo(reputacaoAntes - 5);
    }

    @Test
    void naoVotaDuasVezesNemNaPropriaNemFoiDoPrazo() throws Exception {
        votar(CIDADAO, 7, true);
        mvc.perform(json(como(CIDADAO, post("/ocorrencias/7/confirmacoes")), "{\"aindaExiste\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Você já respondeu sobre esta ocorrência"));

        mvc.perform(json(como(CIDADAO2, post("/ocorrencias/7/confirmacoes")), "{\"aindaExiste\": true}"))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(json(como(CIDADAO2, post("/ocorrencias/1/confirmacoes")), "{\"aindaExiste\": true}"))
                .andExpect(status().isConflict());
    }

    // ---------- Gestor: exceções ----------

    @Test
    void filaDeExcecoesTemAsConfirmadasSemEmissaoAutomatica() throws Exception {
        mvc.perform(como(GESTOR, get("/ocorrencias/excecoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(6))
                .andExpect(jsonPath("$.content[0].categoriaNome").value("Calçada"));
        mvc.perform(como(CIDADAO, get("/ocorrencias/excecoes"))).andExpect(status().isForbidden());
    }

    @Test
    void gestorEmiteOSComExecutorSugerido() throws Exception {
        mvc.perform(como(GESTOR, get("/categorias/4/executor-sugerido")))
                .andExpect(jsonPath("$.nome").value("Calçadas & Cia"));

        String resposta = mvc.perform(como(GESTOR, post("/ocorrencias/6/emitir-os")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"))
                .andReturn().getResponse().getContentAsString();
        Integer osId = JsonPath.read(resposta, "$.osId");
        OrdemServico os = ordens.findById(osId.longValue()).orElseThrow();
        assertThat(os.getExecutor().getNome()).isEqualTo("Calçadas & Cia");
        assertThat(os.getEventos().getFirst().getAutorNome()).isEqualTo("Carlos Lima");

        mvc.perform(como(GESTOR, post("/ocorrencias/6/emitir-os")))
                .andExpect(status().isConflict());
        mvc.perform(como(GESTOR, get("/ocorrencias/excecoes"))).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void gestorEscolheOutroExecutorEPrazo() throws Exception {
        String resposta = mvc.perform(json(como(GESTOR, post("/ocorrencias/6/emitir-os")),
                        "{\"executorId\": 3, \"prazoHoras\": 24}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer osId = JsonPath.read(resposta, "$.osId");
        OrdemServico os = ordens.findById(osId.longValue()).orElseThrow();
        assertThat(os.getExecutor().getId()).isEqualTo(3L);
        assertThat(Duration.between(Instant.now(), os.getPrazoEtapa()).toHours()).isEqualTo(23);
    }

    @Test
    void gestorEncaminhaComProtocolo() throws Exception {
        mvc.perform(json(como(GESTOR, post("/ocorrencias/6/encaminhar")),
                        "{\"orgao\": \"Enel\", \"protocolo\": \"ENEL-778812\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCAMINHADA"))
                .andExpect(jsonPath("$.orgaoEncaminhado").value("Enel"))
                .andExpect(jsonPath("$.protocoloExterno").value("ENEL-778812"));
    }

    @Test
    void descarteExigeJustificativa() throws Exception {
        mvc.perform(json(como(GESTOR, post("/ocorrencias/6/descartar")), "{\"justificativa\": \"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(como(GESTOR, post("/ocorrencias/6/descartar")), "{\"justificativa\": \"Duplicada da 5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESCARTADA"))
                .andExpect(jsonPath("$.motivoDescarte").value("Duplicada da 5"));
        // Descartada é final: não pode mais virar OS.
        mvc.perform(como(GESTOR, post("/ocorrencias/6/emitir-os"))).andExpect(status().isConflict());
    }
}
