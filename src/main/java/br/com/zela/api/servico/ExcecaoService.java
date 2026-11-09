package br.com.zela.api.servico;

import br.com.zela.api.dominio.Ator;
import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.EmissaoOSRequisicao;
import br.com.zela.api.web.dto.EncaminhamentoRequisicao;
import br.com.zela.api.web.dto.ExecutorResposta;
import br.com.zela.api.web.dto.OcorrenciaResposta;
import java.time.Duration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fila de exceções do gestor: o que não virou OS automaticamente (categoria sem emissão automática ou
 * em triagem manual). O gestor só trata exceções; a rotina anda sozinha.
 */
@Service
public class ExcecaoService {

    private final OcorrenciaRepository ocorrencias;
    private final OcorrenciaService ocorrenciaService;
    private final CategoriaService categorias;
    private final ExecutorService executores;
    private final EmissaoOrdemService emissao;
    private final UsuarioLogado logado;

    public ExcecaoService(OcorrenciaRepository ocorrencias, OcorrenciaService ocorrenciaService,
            CategoriaService categorias, ExecutorService executores, EmissaoOrdemService emissao,
            UsuarioLogado logado) {
        this.ocorrencias = ocorrencias;
        this.ocorrenciaService = ocorrenciaService;
        this.categorias = categorias;
        this.executores = executores;
        this.emissao = emissao;
        this.logado = logado;
    }

    /** Maior prioridade primeiro; entre iguais, a mais antiga. */
    @Transactional(readOnly = true)
    public Page<OcorrenciaResposta> listar(Pageable paginacao) {
        return ocorrencias.excecoes(paginacao).map(OcorrenciaResposta::de);
    }

    /** Executor do contrato da categoria (sugestão para o gestor). */
    @Transactional(readOnly = true)
    public ExecutorResposta executorSugerido(Long categoriaId) {
        return ExecutorResposta.de(categorias.carregar(categoriaId).getExecutorResponsavel());
    }

    @Transactional
    public OcorrenciaResposta emitirOS(Long ocorrenciaId, EmissaoOSRequisicao req) {
        Ocorrencia o = ocorrenciaService.carregar(ocorrenciaId);
        var categoria = o.getCategoria();
        Executor executor = req != null && req.executorId() != null
                ? executores.carregar(req.executorId())
                : categoria.getExecutorResponsavel();
        int horas = req != null && req.prazoHoras() != null ? req.prazoHoras() : categoria.getPrazoHoras();
        emissao.emitir(o, executor, Duration.ofHours(horas), Ator.de(logado.usuario()));
        return OcorrenciaResposta.de(o);
    }

    /** Fora da competência da prefeitura: registra o órgão e o protocolo externo. */
    @Transactional
    public OcorrenciaResposta encaminhar(Long ocorrenciaId, EncaminhamentoRequisicao req) {
        Ocorrencia o = semOS(ocorrenciaId);
        o.encaminhar(req.orgao().trim(), req.protocolo().trim());
        return OcorrenciaResposta.de(o);
    }

    @Transactional
    public OcorrenciaResposta descartar(Long ocorrenciaId, String justificativa) {
        Ocorrencia o = semOS(ocorrenciaId);
        o.descartar(justificativa.trim());
        return OcorrenciaResposta.de(o);
    }

    private Ocorrencia semOS(Long id) {
        Ocorrencia o = ocorrenciaService.carregar(id);
        if (o.getOrdemAtual() != null) {
            throw new ConflitoException("A ocorrência já tem a OS %d; trate pela ordem de serviço"
                    .formatted(o.getOrdemAtual().getId()));
        }
        return o;
    }
}
