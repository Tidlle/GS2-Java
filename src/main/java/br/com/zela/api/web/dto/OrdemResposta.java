package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Localizacao;
import br.com.zela.api.dominio.Ocorrencia;
import br.com.zela.api.dominio.OrdemServico;
import br.com.zela.api.dominio.StatusOS;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Ordem de serviço com os mesmos campos do tipo OrdemServico do app, mais um resumo da ocorrência
 * (para o card não precisar de outra chamada). A timeline só vem no detalhe.
 */
public record OrdemResposta(
        Long id,
        Long ocorrenciaId,
        Long executorId,
        String executorNome,
        StatusOS status,
        Instant prazoEtapa,
        boolean atrasada,
        Double checkinLat,
        Double checkinLng,
        String fotoDepoisUrl,
        String observacao,
        Double confiancaIA,
        String protocoloExterno,
        Instant criadaEm,
        Instant concluidaEm,
        boolean retrabalho,
        Boolean cidadaoConfirmou,
        OcorrenciaResumo ocorrencia,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<EventoResposta> timeline) {

    /** Dados da ocorrência que o executor precisa para ir ao local. */
    public record OcorrenciaResumo(
            Long id,
            String titulo,
            String categoriaNome,
            String descricaoIA,
            String bairro,
            String endereco,
            double latitude,
            double longitude,
            String fotoAntesUrl,
            boolean urgente,
            Long autorId) {

        static OcorrenciaResumo de(Ocorrencia o) {
            return new OcorrenciaResumo(o.getId(), o.getTitulo(), o.getCategoria().getNome(), o.getDescricao(),
                    o.getBairro(), o.getEndereco(), o.getLocalizacao().latitude(), o.getLocalizacao().longitude(),
                    o.getFotoAntesUrl(), o.isUrgente(), o.getAutor().getId());
        }
    }

    /** Versão para listas (sem timeline). */
    public static OrdemResposta resumo(OrdemServico os) {
        return montar(os, null);
    }

    /** Versão de detalhe, com a timeline completa. */
    public static OrdemResposta detalhe(OrdemServico os) {
        return montar(os, os.getEventos().stream().map(EventoResposta::de).toList());
    }

    private static OrdemResposta montar(OrdemServico os, List<EventoResposta> timeline) {
        Localizacao checkin = os.getCheckin();
        return new OrdemResposta(os.getId(), os.getOcorrencia().getId(), os.getExecutor().getId(),
                os.getExecutor().getNome(), os.getStatus(), os.getPrazoEtapa(), os.isAtrasada(),
                checkin != null ? checkin.latitude() : null, checkin != null ? checkin.longitude() : null,
                os.getFotoDepoisUrl(), os.getObservacao(), os.getConfiancaIa(), os.getProtocoloExterno(),
                os.getCriadaEm(), os.getConcluidaEm(), os.isRetrabalho(), os.getCidadaoConfirmou(),
                OcorrenciaResumo.de(os.getOcorrencia()), timeline);
    }
}
