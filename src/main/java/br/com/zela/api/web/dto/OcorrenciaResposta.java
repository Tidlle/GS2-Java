package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.StatusOcorrencia;
import java.time.Instant;

/**
 * Mesmos campos do tipo Ocorrencia do app. {@code distanciaM} só vem quando a busca informa a posição.
 * O autor aparece só pelo id (o nome de quem reportou não é exposto aos vizinhos).
 */
public record OcorrenciaResposta(
        Long id,
        Long autorId,
        Long categoriaId,
        String categoriaNome,
        String titulo,
        String descricaoIA,
        String bairro,
        String endereco,
        double latitude,
        double longitude,
        Integer distanciaM,
        String fotoAntesUrl,
        boolean urgente,
        StatusOcorrencia status,
        int prioridade,
        Instant criadaEm,
        int confirmacoesSim,
        int confirmacoesNao,
        Long osId,
        String orgaoEncaminhado,
        String protocoloExterno,
        String motivoDescarte) {

    public static OcorrenciaResposta de(Ocorrencia o) {
        return de(o, null);
    }

    public static OcorrenciaResposta de(Ocorrencia o, Integer distanciaM) {
        return new OcorrenciaResposta(o.getId(), o.getAutor().getId(), o.getCategoria().getId(),
                o.getCategoria().getNome(), o.getTitulo(), o.getDescricao(), o.getBairro(), o.getEndereco(),
                o.getLocalizacao().latitude(), o.getLocalizacao().longitude(), distanciaM, o.getFotoAntesUrl(),
                o.isUrgente(), o.getStatus(), o.getPrioridade(), o.getCriadaEm(), o.getConfirmacoesSim(),
                o.getConfirmacoesNao(), o.getOrdemAtual() != null ? o.getOrdemAtual().getId() : null,
                o.getOrgaoEncaminhado(), o.getProtocoloExterno(), o.getMotivoDescarte());
    }
}
