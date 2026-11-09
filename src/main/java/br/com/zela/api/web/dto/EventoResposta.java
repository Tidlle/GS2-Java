package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.EventoOS;
import br.com.zela.api.dominio.StatusOS;
import java.time.Instant;

/** Item da timeline da OS (mesmos campos do tipo EventoOS do app). */
public record EventoResposta(
        Long id,
        Instant data,
        StatusOS statusAnterior,
        StatusOS statusNovo,
        String usuario,
        String justificativa) {

    public static EventoResposta de(EventoOS e) {
        return new EventoResposta(e.getId(), e.getData(), e.getStatusAnterior(), e.getStatusNovo(),
                e.getAutorNome(), e.getJustificativa());
    }
}
