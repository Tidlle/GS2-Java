package br.com.zela.api.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.zela.api.excecao.TransicaoInvalidaException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrdemServicoTest {

    private final Ator executor = Ator.sistema("Equipe Luz Norte");
    private final Ator gestor = Ator.sistema("Carlos Lima");
    private OrdemServico os;

    @BeforeEach
    void emitir() {
        Executor luzNorte = new Executor("Equipe Luz Norte", TipoExecutor.EMPRESA, "Zona Sul", "Contrato 014/2025");
        Categoria iluminacao = new Categoria("Iluminação", "bulb-outline", luzNorte, 48, true, 2);
        Usuario ana = Usuario.novoCidadao("Ana Souza", "cidadao@zela.com", "hash", "Vila Mariana");
        Ocorrencia poste = new Ocorrencia(ana, iluminacao, "Poste apagado", "Poste apagado à noite.",
                "Vila Mariana", "R. Domingos de Morais, 1200", new Localizacao(-23.5884, -46.6352), null, false);
        os = new OrdemServico(poste, luzNorte, Duration.ofHours(48), Ator.sistema("Sistema (emissão automática)"));
    }

    @Test
    void emissaoComecaEmEmitidaComPrazoEPrimeiroEvento() {
        assertThat(os.getStatus()).isEqualTo(StatusOS.EMITIDA);
        assertThat(os.getPrazoEtapa()).isAfter(Instant.now().plus(Duration.ofHours(47)));
        assertThat(os.getEventos()).singleElement().satisfies(e -> {
            assertThat(e.getStatusAnterior()).isNull();
            assertThat(e.getStatusNovo()).isEqualTo(StatusOS.EMITIDA);
            assertThat(e.getAutorNome()).isEqualTo("Sistema (emissão automática)");
        });
    }

    @Test
    void cicloCompletoGravaUmEventoPorEtapa() {
        os.transicionar(StatusOS.ACEITA, executor, null);
        os.transicionar(StatusOS.EM_EXECUCAO, executor, "Check-in a 12 m do local");
        os.transicionar(StatusOS.CONCLUIDA, executor, "Foto do depois enviada");
        os.transicionar(StatusOS.VALIDADA, gestor, null);
        os.transicionar(StatusOS.ENCERRADA, Ator.sistema(), null);

        assertThat(os.getStatus()).isEqualTo(StatusOS.ENCERRADA);
        assertThat(os.getConcluidaEm()).isNotNull();
        assertThat(os.isRetrabalho()).isFalse();
        assertThat(os.getEventos()).extracting(EventoOS::getStatusNovo).containsExactly(
                StatusOS.EMITIDA, StatusOS.ACEITA, StatusOS.EM_EXECUCAO, StatusOS.CONCLUIDA,
                StatusOS.VALIDADA, StatusOS.ENCERRADA);
        assertThat(os.getEventos().get(2).getStatusAnterior()).isEqualTo(StatusOS.ACEITA);
        assertThat(os.getEventos().get(2).getJustificativa()).isEqualTo("Check-in a 12 m do local");
    }

    @Test
    void naoPulaEtapas() {
        assertThatThrownBy(() -> os.transicionar(StatusOS.CONCLUIDA, executor, null))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("OS não pode passar de EMITIDA para CONCLUIDA");
        assertThat(os.getStatus()).isEqualTo(StatusOS.EMITIDA);
        assertThat(os.getEventos()).hasSize(1);
    }

    @Test
    void osEncerradaNaoMudaMais() {
        os.transicionar(StatusOS.ENCERRADA, gestor, "Cancelada: duplicada");
        assertThat(StatusOS.ENCERRADA.isFinal()).isTrue();
        assertThatThrownBy(() -> os.transicionar(StatusOS.ACEITA, executor, null))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void reprovacaoReabreEMarcaRetrabalho() {
        os.transicionar(StatusOS.ACEITA, executor, null);
        os.transicionar(StatusOS.EM_EXECUCAO, executor, null);
        os.transicionar(StatusOS.CONCLUIDA, executor, null);
        os.transicionar(StatusOS.REABERTA, gestor, "Reparo parcial");

        assertThat(os.isRetrabalho()).isTrue();
        // Depois de reaberta, o executor volta ao local para refazer.
        os.transicionar(StatusOS.EM_EXECUCAO, executor, null);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void recusaPodeSerEncaminhadaAOutroOrgao() {
        os.transicionar(StatusOS.RECUSADA, executor, "Fora da competência");
        os.transicionar(StatusOS.ENCAMINHADA, gestor, "Protocolo DER-2026-004871");
        assertThat(os.getStatus().isFinal()).isTrue();
    }

    @Test
    void escaladaPodeSerReatribuida() {
        os.transicionar(StatusOS.ESCALADA, Ator.sistema(), "Prazo de aceite estourado");
        os.transicionar(StatusOS.EMITIDA, gestor, "Reatribuída pelo supervisor");
        assertThat(os.getStatus()).isEqualTo(StatusOS.EMITIDA);
    }

    @Test
    void eventoSemMudancaDeStatusFicaNaTimeline() {
        os.registrarEvento(gestor, "Prazo da etapa alterado para 12 h");
        EventoOS ultimo = os.getEventos().getLast();
        assertThat(ultimo.getStatusAnterior()).isNull();
        assertThat(ultimo.getStatusNovo()).isEqualTo(StatusOS.EMITIDA);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EMITIDA);
    }

    @Test
    void atrasadaQuandoPrazoPassouEmEtapaAberta() {
        assertThat(os.isAtrasada()).isFalse();
        os.definirPrazo(Duration.ofHours(-1));
        assertThat(os.isAtrasada()).isTrue();
    }

    @Test
    void cadaMudancaDeStatusGeraUmEventoDeDominioPublicadoUmaVez() {
        os.transicionar(StatusOS.ACEITA, executor, null);
        os.registrarEvento(gestor, "Prazo alterado"); // sem mudança de status: não vira mensagem
        os.transicionar(StatusOS.EM_EXECUCAO, executor, "Check-in a 8 m do local");

        assertThat(os.retirarEventos()).extracting(e -> e.tipo() + "|" + e.anterior() + "|" + e.detalhe())
                .containsExactly("os.emitida|null|null", "os.aceita|EMITIDA|null",
                        "os.em_execucao|ACEITA|Check-in a 8 m do local");
        assertThat(os.retirarEventos()).isEmpty();
    }

    @Test
    void timelineNaoPodeSerAlteradaDeFora() {
        assertThatThrownBy(() -> os.getEventos().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
