package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Notificacao;
import java.time.Instant;

/** Mesmos campos do tipo Notificacao do app. */
public record NotificacaoResposta(Long id, String texto, Instant data, boolean lida) {

    public static NotificacaoResposta de(Notificacao n) {
        return new NotificacaoResposta(n.getId(), n.getTexto(), n.getCriadaEm(), n.isLida());
    }
}
