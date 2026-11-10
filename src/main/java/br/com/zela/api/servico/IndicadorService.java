package br.com.zela.api.servico;

import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import br.com.zela.api.repositorio.AvaliacaoRepository;
import br.com.zela.api.repositorio.ExecutorRepository;
import br.com.zela.api.repositorio.OcorrenciaRepository;
import br.com.zela.api.repositorio.OrdemServicoRepository;
import br.com.zela.api.web.dto.BairroIndicador;
import br.com.zela.api.web.dto.ExecutorIndicador;
import br.com.zela.api.web.dto.IndicadoresResposta;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores para fiscalizar a execução e os contratos.
 *
 * <ul>
 *   <li><b>Concluída</b>: OS que chegou a ter a foto do depois (tem data de conclusão).</li>
 *   <li><b>No prazo</b>: concluída antes do prazo da etapa.</li>
 *   <li><b>Retrabalho</b>: reprovada pelo gestor ou contestada pelo cidadão ao menos uma vez.</li>
 *   <li><b>Tempo médio</b>: horas da emissão da OS até a conclusão.</li>
 * </ul>
 * Calculado em memória sobre as OS (volume de uma subprefeitura). Em escala maior, viraria consulta agregada no banco.
 */
@Service
public class IndicadorService {

    private final OrdemServicoRepository ordens;
    private final OcorrenciaRepository ocorrencias;
    private final ExecutorRepository executores;
    private final AvaliacaoRepository avaliacoes;

    public IndicadorService(OrdemServicoRepository ordens, OcorrenciaRepository ocorrencias,
            ExecutorRepository executores, AvaliacaoRepository avaliacoes) {
        this.ordens = ordens;
        this.ocorrencias = ocorrencias;
        this.executores = executores;
        this.avaliacoes = avaliacoes;
    }

    @Transactional(readOnly = true)
    public IndicadoresResposta painel() {
        List<OrdemServico> todas = ordens.findAllByOrderByIdAsc();
        List<OrdemServico> concluidas = concluidas(todas);
        return new IndicadoresResposta(
                todas.stream().filter(os -> os.getStatus().isAberta()).count(),
                todas.stream().filter(OrdemServico::isAtrasada).count(),
                todas.stream().filter(os -> os.getStatus() == StatusOS.ESCALADA).count(),
                concluidas.size(),
                noPrazoPct(concluidas),
                retrabalhoPct(concluidas),
                tempoMedioHoras(concluidas),
                ocorrencias.excecoes(Pageable.ofSize(1)).getTotalElements(),
                bairros(concluidas));
    }

    /** Ranking de executores pelo cumprimento de prazo (melhores primeiro). */
    @Transactional(readOnly = true)
    public List<ExecutorIndicador> executores() {
        Map<Long, List<OrdemServico>> porExecutor = ordens.findAllByOrderByIdAsc().stream()
                .collect(Collectors.groupingBy(os -> os.getExecutor().getId()));
        Map<Long, double[]> notas = new HashMap<>();
        for (Object[] linha : avaliacoes.mediaDeNotasPorExecutor()) {
            notas.put((Long) linha[0], new double[] {((Number) linha[1]).doubleValue(), ((Number) linha[2]).doubleValue()});
        }
        return executores.findAll().stream()
                .map(e -> indicador(e, porExecutor.getOrDefault(e.getId(), List.of()), notas.get(e.getId())))
                .sorted(Comparator.comparing(ExecutorIndicador::prazosCumpridosPct,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BairroIndicador> bairros() {
        return bairros(concluidas(ordens.findAllByOrderByIdAsc()));
    }

    // ---------- Cálculos ----------

    private ExecutorIndicador indicador(Executor e, List<OrdemServico> os, double[] nota) {
        List<OrdemServico> concluidas = concluidas(os);
        return new ExecutorIndicador(e.getId(), e.getNome(), e.getTipo(), e.getRegiao(), e.getContrato(),
                os.size(),
                os.stream().filter(o -> o.getStatus().isAberta()).count(),
                os.stream().filter(OrdemServico::isAtrasada).count(),
                concluidas.size(),
                tempoMedioHoras(concluidas),
                noPrazoPct(concluidas),
                retrabalhoPct(concluidas),
                nota != null ? Math.round(nota[0] * 10) / 10.0 : null,
                nota != null ? (long) nota[1] : 0);
    }

    /** Bairros do mais rápido para o mais lento (transparência e redução de desigualdades: ODS 10). */
    private List<BairroIndicador> bairros(List<OrdemServico> concluidas) {
        Map<String, List<OrdemServico>> resolvidas = concluidas.stream()
                .collect(Collectors.groupingBy(os -> os.getOcorrencia().getBairro()));
        return ocorrencias.contagemPorBairro().stream()
                .map(linha -> {
                    String bairro = (String) linha[0];
                    List<OrdemServico> doBairro = resolvidas.getOrDefault(bairro, List.of());
                    return new BairroIndicador(bairro, ((Number) linha[1]).longValue(), doBairro.size(),
                            media(doBairro.stream().mapToDouble(os ->
                                    horas(Duration.between(os.getOcorrencia().getCriadaEm(), os.getConcluidaEm())))
                                    .average()));
                })
                .sorted(Comparator.comparing(BairroIndicador::horas, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static List<OrdemServico> concluidas(List<OrdemServico> os) {
        return os.stream().filter(o -> o.getConcluidaEm() != null).toList();
    }

    private static Integer noPrazoPct(List<OrdemServico> concluidas) {
        return pct(concluidas.stream().filter(o -> !o.getConcluidaEm().isAfter(o.getPrazoEtapa())).count(),
                concluidas.size());
    }

    private static Integer retrabalhoPct(List<OrdemServico> concluidas) {
        return pct(concluidas.stream().filter(OrdemServico::isRetrabalho).count(), concluidas.size());
    }

    private static Double tempoMedioHoras(List<OrdemServico> concluidas) {
        return media(concluidas.stream()
                .mapToDouble(o -> horas(Duration.between(o.getCriadaEm(), o.getConcluidaEm()))).average());
    }

    private static Integer pct(long parte, long total) {
        return total == 0 ? null : (int) Math.round(100.0 * parte / total);
    }

    private static double horas(Duration d) {
        return d.toMinutes() / 60.0;
    }

    private static Double media(OptionalDouble m) {
        return m.isPresent() ? Math.round(m.getAsDouble() * 10) / 10.0 : null;
    }
}
