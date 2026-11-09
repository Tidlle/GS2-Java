package br.com.zela.api.web;

import br.com.zela.api.servico.AuthService;
import br.com.zela.api.web.dto.CadastroRequisicao;
import br.com.zela.api.web.dto.LoginRequisicao;
import br.com.zela.api.web.dto.LoginResposta;
import br.com.zela.api.web.dto.UsuarioResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Login com JWT e cadastro de cidadão")
@SecurityRequirements
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Login", description = "Devolve o JWT para usar em Authorization: Bearer <token>.")
    @ApiResponse(responseCode = "200", description = "Login feito")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    @PostMapping("/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao req) {
        return auth.login(req);
    }

    @Operation(summary = "Cadastro de cidadão", description = "Cria uma conta com o papel CIDADAO.")
    @ApiResponse(responseCode = "201", description = "Conta criada")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResposta> cadastrar(@Valid @RequestBody CadastroRequisicao req) {
        UsuarioResposta criado = auth.cadastrar(req);
        return ResponseEntity.created(URI.create("/usuarios/me")).body(criado);
    }
}
