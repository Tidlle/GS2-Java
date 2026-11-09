package br.com.zela.api.servico;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Confirmacao;
import br.com.zela.api.dominio.Localizacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.RegraNegocioException;
import br.com.zela.api.repositorio.ConfirmacaoRepository;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.OcorrenciaResposta;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Esse problema ainda está aqui?". Os vizinhos confirmam ou negam:
 * <ul>
 *   <li>cada "sim" aumenta a prioridade e a reputação de quem reportou;</li>
 *   <li>N "sim" deixam a ocorrência CONFIRMADA: categoria de rotina vira OS na hora, as demais vão às exceções;</li>
 *   <li>N "não" descartam a ocorrência (report falso ou já resolvido) e reduzem a reputação do autor.</li>
 * </ul>
 * Sem resposta, a ocorrência segue sozinha depois de 24 h (agendador da Fase 8): a confirmação nunca bloqueia.
 */
@Service
public class ConfirmacaoService {

    static final int REPUTACAO_POR_CONFIRMACAO = 1;
    static final int REPUTACAO_POR_DESCARTE = -5;

    private final OcorrenciaRepository ocorrencias;
    private final ConfirmacaoRepository confirmacoes;
    private final OcorrenciaService ocorrenciaService;
    private final EmissaoOrdemService emissao;
    private final UsuarioLogado logado;
    private final ZelaProperties propriedades;

    public ConfirmacaoService(OcorrenciaRepository ocorrencias, ConfirmacaoRepository confirmacoes,
            OcorrenciaService ocorrenciaService, EmissaoOrdemService emissao, UsuarioLogado logado,
            ZelaProperties propriedades) {
        this.ocorrencias = ocorrencias;
        this.confirmacoes = confirmacoes;
        this.ocorrenciaService = ocorrenciaService;
        this.emissao = emissao;
        this.logado = logado;
        this.propriedades = propriedades;
    }

    /** Ocorrências de outros cidadãos aguardando confirmação, ainda não respondidas; com posição, as mais próximas primeiro. */
    @Transactional(readOnly = true)
    public List<OcorrenciaResposta> paraConfirmar(Double lat, Double lng) {
        List<Ocorrencia> pendentes = ocorrencias.paraConfirmar(logado.id());
        if (lat == null || lng == null) {
            return pendentes.stream().map(OcorrenciaResposta::de).toList();
        }
        Localizacao aqui = new Localizacao(lat, lng);
        int raio = propriedades.regras().raioPadraoMetros();
        return pendentes.stream()
                .map(o -> OcorrenciaResposta.de(o, (int) Math.round(aqui.distanciaMetros(o.getLocalizacao()))))
                .filter(r -> r.distanciaM() <= raio)
                .sorted(Comparator.comparingInt(OcorrenciaResposta::distanciaM))
                .toList();
    }

    @Transactional
    public OcorrenciaResposta responder(Long ocorrenciaId, boolean aindaExiste) {
        Usuario vizinho = logado.usuario();
        Ocorrencia o = ocorrenciaService.carregar(ocorrenciaId);

        if (o.getAutor().getId().equals(vizinho.getId())) {
            throw new RegraNegocioException("Quem reportou não confirma a própria ocorrência");
        }
        if (o.getStatus() != StatusOcorrencia.AGUARDANDO_CONFIRMACAO) {
            throw new ConflitoException("A ocorrência não está aguardando confirmação (status atual: %s)"
                    .formatted(o.getStatus()));
        }
        if (confirmacoes.existsByOcorrenciaIdAndUsuarioId(o.getId(), vizinho.getId())) {
            throw new ConflitoException("Você já respondeu sobre esta ocorrência");
        }

        confirmacoes.save(new Confirmacao(o, vizinho, aindaExiste));
        o.registrarConfirmacao(aindaExiste);
        if (aindaExiste) {
            o.getAutor().ajustarReputacao(REPUTACAO_POR_CONFIRMACAO);
        }

        var regras = propriedades.regras();
        if (o.getConfirmacoesSim() >= regras.confirmacoesParaConfirmar()) {
            o.transicionar(StatusOcorrencia.CONFIRMADA);
            if (o.getCategoria().isEmissaoAutomatica()) {
                emissao.emitirAutomaticamente(o);
            }
        } else if (o.getConfirmacoesNao() >= regras.negacoesParaDescartar()) {
            o.descartar("Negada pelos vizinhos: %d responderam que o problema não existe mais"
                    .formatted(o.getConfirmacoesNao()));
            o.getAutor().ajustarReputacao(REPUTACAO_POR_DESCARTE);
        }
        return OcorrenciaResposta.de(o);
    }
}
