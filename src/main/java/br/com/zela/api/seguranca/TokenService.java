package br.com.zela.api.seguranca;

import br.com.zela.api.config.ZelaProperties;
import br.com.zela.api.dominio.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Gera o JWT entregue no login. */
@Service
public class TokenService {

    public static final String EMISSOR = "zela-api";

    private final JwtEncoder encoder;
    private final Duration validade;

    public TokenService(JwtEncoder encoder, ZelaProperties propriedades) {
        this.encoder = encoder;
        this.validade = Duration.ofHours(propriedades.jwt().expiracaoHoras());
    }

    /**
     * Claims: sub = id do usuário, papel = papel de acesso, nome e email só para exibição.
     * Nenhum dado sensível (senha, hash) vai no token.
     */
    public TokenGerado gerar(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(JwtConfig.CLAIM_PAPEL, usuario.getPapel().name())
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenGerado(token, expiraEm);
    }

    public record TokenGerado(String valor, Instant expiraEm) {
    }
}
