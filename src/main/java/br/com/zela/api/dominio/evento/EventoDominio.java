package br.com.zela.api.dominio.evento;

import java.time.Instant;

/**
 * Mudança de status de uma OS ou de uma ocorrência. Carrega só ids e textos (nada de entidades),
 * para poder virar uma mensagem JSON e ser processada em outra transação.
 *
 * @param tipo         chave de roteamento no RabbitMQ, ex.: "os.escalada", "ocorrencia.urgente"
 * @param ocorrenciaId ocorrência envolvida
 * @param osId         OS envolvida (vazio em eventos só da ocorrência)
 * @param anterior     status anterior (vazio na criação)
 * @param novo         novo status
 * @param autor        quem causou (nome exibido na timeline)
 * @param detalhe      justificativa ou informação extra
 * @param quando       momento do fato
 */
public record EventoDominio(
        String tipo,
        Long ocorrenciaId,
        Long osId,
        String anterior,
        String novo,
        String autor,
        String detalhe,
        Instant quando) {

    public static EventoDominio daOS(Long osId, Long ocorrenciaId, Enum<?> anterior, Enum<?> novo, String autor,
            String detalhe) {
        return new EventoDominio("os." + novo.name().toLowerCase(), ocorrenciaId, osId,
                anterior != null ? anterior.name() : null, novo.name(), autor, detalhe, Instant.now());
    }

    public static EventoDominio daOcorrencia(Long ocorrenciaId, Enum<?> anterior, Enum<?> novo, String detalhe) {
        return new EventoDominio("ocorrencia." + novo.name().toLowerCase(), ocorrenciaId, null,
                anterior != null ? anterior.name() : null, novo.name(), null, detalhe, Instant.now());
    }
}
