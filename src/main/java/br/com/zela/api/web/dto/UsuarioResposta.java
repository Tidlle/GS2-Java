package br.com.zela.api.web.dto;

import br.com.zela.api.dominio.Executor;
import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.TipoExecutor;
import br.com.zela.api.dominio.Usuario;

/** Dados públicos do usuário (nunca inclui a senha). Mesmos campos do tipo Usuario do app. */
public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        Papel papel,
        String bairro,
        Integer reputacao,
        TipoExecutor tipo,
        Long executorId) {

    public static UsuarioResposta de(Usuario u) {
        Executor e = u.getExecutor();
        return new UsuarioResposta(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.getBairro(),
                u.getPapel() == Papel.CIDADAO ? u.getReputacao() : null,
                e != null ? e.getTipo() : null,
                e != null ? e.getId() : null);
    }
}
