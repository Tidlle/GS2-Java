package br.com.zela.api.servico;

import br.com.zela.api.dominio.Usuario;
import br.com.zela.api.excecao.ConflitoException;
import br.com.zela.api.excecao.CredenciaisInvalidasException;
import br.com.zela.api.repositorio.UsuarioRepository;
import br.com.zela.api.seguranca.LimiteDeTentativasDeLogin;
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
    private final LimiteDeTentativasDeLogin tentativas;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens,
            LimiteDeTentativasDeLogin tentativas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.tentativas = tentativas;
    }

    /**
     * Confere e-mail e senha (hash BCrypt) e devolve um JWT. Mensagem única para não revelar se o e-mail existe.
     * Depois de 5 erros seguidos para o mesmo e-mail, bloqueia por 15 minutos (429).
     */
    @Transactional(readOnly = true)
    public LoginResposta login(LoginRequisicao req) {
        String email = req.email().trim();
        tentativas.verificar(email);
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .filter(u -> senhas.matches(req.senha(), u.getSenhaHash()))
                .orElse(null);
        if (usuario == null) {
            tentativas.registrarFalha(email);
            throw new CredenciaisInvalidasException();
        }
        tentativas.registrarSucesso(email);
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
