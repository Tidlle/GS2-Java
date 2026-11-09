package br.com.zela.api.web.dto;

import java.time.Instant;

/** Resposta do login: o token vai no cabeçalho Authorization das próximas chamadas. */
public record LoginResposta(String token, String tipo, Instant expiraEm, UsuarioResposta usuario) {

    public static LoginResposta bearer(String token, Instant expiraEm, UsuarioResposta usuario) {
        return new LoginResposta(token, "Bearer", expiraEm, usuario);
    }
}
