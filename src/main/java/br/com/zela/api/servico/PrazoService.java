package br.com.zela.api.servico;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Ator;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de prazo do Zela. Cada item é tratado na sua própria transação: se um falhar, os outros seguem.
 * Antes de agir, o estado é conferido de novo (pode ter mudado desde a busca).
 *
 * <ul>
 *   <li><b>Confirmação que não bloqueia</b>: sem resposta dos vizinhos em 24 h, a ocorrência segue sozinha.</li>
 *   <li><b>Escalonamento</b>: etapa com prazo estourado vira ESCALADA e avisa supervisor e cidadão.</li>
 *   <li><b>Aceite tácito</b>: OS concluída com confiança da IA ≥ 0,8 e sem contestação em 48 h é encerrada.</li>
 * </ul>
 */
@Service
public class PrazoService {

    /** Etapas que podem estourar o prazo. CONCLUIDA fica de fora: a validação tem a regra do aceite tácito. */
    static final EnumSet<StatusOS> ETAPAS_COM_PRAZO =
            EnumSet.of(StatusOS.EMITIDA, StatusOS.ACEITA, StatusOS.EM_EXECUCAO, StatusOS.REABERTA);

    private final OcorrenciaRepository ocorrencias;
    private final OrdemServicoRepository ordens;
    private final EmissaoOrdemService emissao;
    private final OrdemServicoService ordemServico;
    private final ZelaProperties propriedades;

    public PrazoService(OcorrenciaRepository ocorrencias, OrdemServicoRepository ordens, EmissaoOrdemService emissao,
            OrdemServicoService ordemServico, ZelaProperties propriedades) {
        this.ocorrencias = ocorrencias;
        this.ordens = ordens;
        this.emissao = emissao;
        this.ordemServico = ordemServico;
        this.propriedades = propriedades;
    }

    // ---------- Buscas ----------

    @Transactional(readOnly = true)
    public List<Long> semConfirmacaoNoPrazo() {
        Instant limite = Instant.now().minus(Duration.ofHours(propriedades.regras().horasSemConfirmacao()));
        return ocorrencias.idsAguardandoDesde(limite);
    }

    @Transactional(readOnly = true)
    public List<Long> comPrazoEstourado() {
        return ordens.idsParaEscalar(ETAPAS_COM_PRAZO, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<Long> prontasParaAceiteTacito() {
        var regras = propriedades.regras();
        Instant limite = Instant.now().minus(Duration.ofHours(regras.horasAceiteTacito()));
        return ordens.idsParaAceiteTacito(limite, regras.confiancaAceiteTacito());
    }

    // ---------- Ações (uma transação por item) ----------

    /** Devolve true se a ocorrência seguiu (e se virou OS automática, a OS já foi emitida). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean seguirSemConfirmacao(Long ocorrenciaId) {
        Ocorrencia o = ocorrencias.findById(ocorrenciaId).orElse(null);
        if (o == null || o.getStatus() != StatusOcorrencia.AGUARDANDO_CONFIRMACAO) {
            return false;
        }
        o.seguirSemConfirmacao("sem resposta dos vizinhos em %d h".formatted(propriedades.regras().horasSemConfirmacao()));
        if (o.getCategoria().isEmissaoAutomatica()) {
            emissao.emitirAutomaticamente(o);
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean escalar(Long osId) {
        OrdemServico os = ordens.findById(osId).orElse(null);
        if (os == null || !ETAPAS_COM_PRAZO.contains(os.getStatus()) || !os.getPrazoEtapa().isBefore(Instant.now())) {
            return false;
        }
        StatusOS etapa = os.getStatus();
        os.transicionar(StatusOS.ESCALADA, Ator.sistema("Sistema (prazos)"),
                "Prazo da etapa %s estourado".formatted(etapa));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean aceitarTacitamente(Long osId) {
        OrdemServico os = ordens.findById(osId).orElse(null);
        var regras = propriedades.regras();
        if (os == null || os.getStatus() != StatusOS.CONCLUIDA || os.getCidadaoConfirmou() != null
                || os.getConfiancaIa() == null || os.getConfiancaIa() < regras.confiancaAceiteTacito()) {
            return false;
        }
        String rotulo = "Aceite tácito (IA %d%%, sem contestação em %d h)"
                .formatted(Math.round(os.getConfiancaIa() * 100), regras.horasAceiteTacito());
        ordemServico.validarEEncerrar(os, Ator.sistema(rotulo), null);
        return true;
    }
}
