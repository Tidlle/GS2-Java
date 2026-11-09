package br.com.zela.api.web;

import br.com.zela.api.seguranca.UsuarioLogado;
import br.com.zela.api.web.dto.UsuarioResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários", description = "Dados do usuário logado")
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioLogado logado;

    public UsuarioController(UsuarioLogado logado) {
        this.logado = logado;
    }

    @Operation(summary = "Usuário logado", description = "Dados do dono do token (perfil no app).")
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UsuarioResposta eu() {
        return UsuarioResposta.de(logado.usuario());
    }
}
