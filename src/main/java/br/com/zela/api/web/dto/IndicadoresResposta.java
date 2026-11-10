package br.com.zela.api.web.dto;

import java.util.List;

/**
 * Painel do gestor e do supervisor (mesmos campos de calcularIndicadores do app, mais alguns totais).
 * Percentuais e médias ficam vazios quando ainda não há OS concluída para calcular.
 */
public record IndicadoresResposta(
        long abertas,
        long atrasadas,
        long escaladas,
        long concluidas,
        Integer prazosCumpridosPct,
        Integer retrabalhoPct,
        Double tempoMedioHoras,
        long excecoes,
        List<BairroIndicador> bairros) {
}
