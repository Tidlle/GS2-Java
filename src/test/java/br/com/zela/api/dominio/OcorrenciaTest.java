package br.com.zela.api.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.zela.api.excecao.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

class OcorrenciaTest {

    private final Executor executor = new Executor("Calçadas & Cia", TipoExecutor.EMPRESA, "Zona Sul", "Contrato 031");
    private final Categoria calcada = new Categoria("Calçada", "walk-outline", executor, 120, false, 2);
    private final Usuario ana = Usuario.novoCidadao("Ana Souza", "Cidadao@Zela.com ", "hash", "Vila Mariana");

    private Ocorrencia nova(boolean urgente) {
        return new Ocorrencia(ana, calcada, "Calçada quebrada", "Lajotas soltas.", "Vila Mariana",
                "R. Pelotas, 98", new Localizacao(-23.5935, -46.6304), null, urgente);
    }

    @Test
    void prioridadeInicialVemDoRiscoDaCategoria() {
        Ocorrencia o = nova(false);
        assertThat(o.getStatus()).isEqualTo(StatusOcorrencia.REPORTADA);
        assertThat(o.getPrioridade()).isEqualTo(2);
    }

    @Test
    void urgenteComecaComPrioridadeMaxima() {
        assertThat(nova(true).getPrioridade()).isEqualTo(Ocorrencia.PRIORIDADE_MAXIMA);
    }

    @Test
    void confirmacaoAumentaPrioridadeAteOMaximo() {
        Ocorrencia o = nova(false);
        for (int i = 0; i < 10; i++) {
            o.registrarConfirmacao(true);
        }
        o.registrarConfirmacao(false);
        assertThat(o.getConfirmacoesSim()).isEqualTo(10);
        assertThat(o.getConfirmacoesNao()).isEqualTo(1);
        assertThat(o.getPrioridade()).isEqualTo(Ocorrencia.PRIORIDADE_MAXIMA);
    }

    @Test
    void fluxoNormalAteResolvida() {
        Ocorrencia o = nova(false);
        o.transicionar(StatusOcorrencia.AGUARDANDO_CONFIRMACAO);
        o.transicionar(StatusOcorrencia.CONFIRMADA);
        o.transicionar(StatusOcorrencia.EM_ATENDIMENTO);
        o.transicionar(StatusOcorrencia.RESOLVIDA);
        assertThat(o.getStatus().isFinal()).isTrue();
    }

    @Test
    void naoVoltaDeDescartada() {
        Ocorrencia o = nova(false);
        o.transicionar(StatusOcorrencia.AGUARDANDO_CONFIRMACAO);
        o.descartar("Negada pelos vizinhos");
        assertThat(o.getMotivoDescarte()).isEqualTo("Negada pelos vizinhos");
        assertThatThrownBy(() -> o.transicionar(StatusOcorrencia.CONFIRMADA))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("Ocorrência não pode passar de DESCARTADA para CONFIRMADA");
    }

    @Test
    void encaminharGuardaOrgaoEProtocolo() {
        Ocorrencia o = nova(false);
        o.transicionar(StatusOcorrencia.EM_TRIAGEM);
        o.encaminhar("DER-SP", "DER-2026-004871");
        assertThat(o.getStatus()).isEqualTo(StatusOcorrencia.ENCAMINHADA);
        assertThat(o.getProtocoloExterno()).isEqualTo("DER-2026-004871");
    }

    @Test
    void autorSoEditaAntesDeVirarOS() {
        Ocorrencia o = nova(false);
        o.transicionar(StatusOcorrencia.AGUARDANDO_CONFIRMACAO);
        assertThat(o.podeSerEditadaPeloAutor()).isTrue();
        o.vincularOrdem(new OrdemServico(o, executor, java.time.Duration.ofHours(120), Ator.sistema()));
        assertThat(o.podeSerEditadaPeloAutor()).isFalse();
    }

    @Test
    void emailENormalizadoEReputacaoFicaEntreZeroECem() {
        assertThat(ana.getEmail()).isEqualTo("cidadao@zela.com");
        ana.ajustarReputacao(500);
        assertThat(ana.getReputacao()).isEqualTo(Usuario.REPUTACAO_MAXIMA);
        ana.ajustarReputacao(-500);
        assertThat(ana.getReputacao()).isZero();
    }
}
