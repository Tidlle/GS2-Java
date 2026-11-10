package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.TipoExecutor;

/**
 * Desempenho do executor (base para fiscalizar o contrato). Mesmos campos do tipo Executor do app.
 *
 * @param tempoMedioHoras    da emissão da OS até a conclusão
 * @param prazosCumpridosPct OS concluídas dentro do prazo da etapa
 * @param retrabalhoPct      OS reprovadas ou contestadas ao menos uma vez
 * @param avaliacao          média das notas dos cidadãos (1 a 5)
 */
public record ExecutorIndicador(
        Long id,
        String nome,
        TipoExecutor tipo,
        String regiao,
        String contrato,
        long totalOS,
        long abertas,
        long atrasadas,
        long concluidas,
        Double tempoMedioHoras,
        Integer prazosCumpridosPct,
        Integer retrabalhoPct,
        Double avaliacao,
        long avaliacoes) {
}
