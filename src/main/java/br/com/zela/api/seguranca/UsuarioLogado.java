package br.com.zela.api.seguranca;

import br.com.zela.api.dominio.Papel;
import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.repositorio.UsuarioRepository;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Acesso ao usuário da requisição atual, a partir do JWT já validado pelo Spring Security. */
@Component
public class UsuarioLogado {

    private final UsuarioRepository usuarios;

    public UsuarioLogado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwt)) {
            throw new InsufficientAuthenticationException("Requisição sem usuário autenticado");
        }
        return Long.valueOf(jwt.getName());
    }

    /** Carrega o usuário do banco (o token pode sobreviver a um usuário removido: nesse caso, 401). */
    public Usuario usuario() {
        return usuarios.findById(id())
                .orElseThrow(() -> new InsufficientAuthenticationException("Usuário do token não existe mais"));
    }

    public boolean tem(Papel papel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + papel.name()));
    }
}
