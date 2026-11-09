package br.com.zela.api.servico;

import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.CredenciaisInvalidasException;
import br.com.zela.api.repositorio.UsuarioRepository;
import br.com.zela.api.seguranca.TokenService;
import br.com.zela.api.seguranca.TokenService.TokenGerado;
import br.com.zela.api.web.dto.CadastroRequisicao;
import br.com.zela.api.web.dto.LoginRequisicao;
import br.com.zela.api.web.dto.LoginResposta;
import br.com.zela.api.web.dto.UsuarioResposta;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login e cadastro. */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    /** Confere e-mail e senha (hash BCrypt) e devolve um JWT. Mensagem única para não revelar se o e-mail existe. */
    @Transactional(readOnly = true)
    public LoginResposta login(LoginRequisicao req) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> senhas.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);
        TokenGerado token = tokens.gerar(usuario);
        return LoginResposta.bearer(token.valor(), token.expiraEm(), UsuarioResposta.de(usuario));
    }

    /** Cadastro público: sempre cria um CIDADAO. Gestores, executores e supervisores vêm da carga inicial. */
    @Transactional
    public UsuarioResposta cadastrar(CadastroRequisicao req) {
        if (usuarios.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflitoException("Já existe uma conta com esse e-mail");
        }
        Usuario novo = Usuario.novoCidadao(req.nome().trim(), req.email(), senhas.encode(req.senha()),
                req.bairro().trim());
        return UsuarioResposta.de(usuarios.save(novo));
    }
}
