package br.com.zela.api.servico;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Ator;
import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOcorrencia;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emissão de ordens de serviço. Três caminhos levam aqui:
 * <ul>
 *   <li>emissão automática (categoria de rotina confirmada pelos vizinhos);</li>
 *   <li>caminho de urgência (pula a confirmação e vai ao plantão com prazo de horas);</li>
 *   <li>gestor, a partir da fila de exceções.</li>
 * </ul>
 */
@Service
public class EmissaoOrdemService {

    private final OrdemServicoRepository ordens;
    private final ZelaProperties propriedades;

    public EmissaoOrdemService(OrdemServicoRepository ordens, ZelaProperties propriedades) {
        this.ordens = ordens;
        this.propriedades = propriedades;
    }

    /** Emite a OS para o executor do contrato da categoria, com o prazo contratual. */
    @Transactional
    public OrdemServico emitirAutomaticamente(Ocorrencia ocorrencia) {
        var categoria = ocorrencia.getCategoria();
        return emitir(ocorrencia, categoria.getExecutorResponsavel(), Duration.ofHours(categoria.getPrazoHoras()),
                Ator.sistema("Sistema (emissão automática)"));
    }

    /** Caminho de urgência: OS imediata ao executor da categoria, com prazo de plantão (horas). */
    @Transactional
    public OrdemServico emitirUrgencia(Ocorrencia ocorrencia) {
        return emitir(ocorrencia, ocorrencia.getCategoria().getExecutorResponsavel(),
                Duration.ofHours(propriedades.regras().prazoUrgenciaHoras()),
                Ator.sistema("Sistema (urgência — plantão)"));
    }

    @Transactional
    public OrdemServico emitir(Ocorrencia ocorrencia, Executor executor, Duration prazo, Ator ator) {
        if (ocorrencia.getOrdemAtual() != null) {
            throw new ConflitoException("A ocorrência %d já tem a OS %d em andamento"
                    .formatted(ocorrencia.getId(), ocorrencia.getOrdemAtual().getId()));
        }
        OrdemServico os = ordens.save(new OrdemServico(ocorrencia, executor, prazo, ator));
        ocorrencia.vincularOrdem(os);
        // A ocorrência urgente continua marcada como URGENTE enquanto a OS do plantão anda.
        if (ocorrencia.getStatus() != StatusOcorrencia.URGENTE) {
            ocorrencia.transicionar(StatusOcorrencia.EM_ATENDIMENTO);
        }
        return os;
    }
}
