package br.com.zela.api.seguranca;

import br.com.zela.api.config.ZelaProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Peças do JWT. O token é assinado com HMAC-SHA256 usando o segredo JWT_SECRET;
 * a mesma chave valida a assinatura em cada requisição.
 */
@Configuration
public class JwtConfig {

    /** Claim do token que carrega o papel do usuário (CIDADAO, GESTOR, EXECUTOR, SUPERVISOR). */
    public static final String CLAIM_PAPEL = "papel";

    private final SecretKey chave;

    public JwtConfig(ZelaProperties propriedades) {
        byte[] segredo = propriedades.jwt().secret().getBytes(StandardCharsets.UTF_8);
        this.chave = new SecretKeySpec(segredo, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /** Confere assinatura, validade (exp) e emissor (iss = zela-api): token de outro sistema é recusado. */
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.EMISSOR));
        return decoder;
    }

    /** Converte o claim "papel" em autoridade ROLE_*, usada em hasRole('GESTOR'), e o "sub" no nome do usuário. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName(CLAIM_PAPEL);
        papeis.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        conversor.setPrincipalClaimName("sub");
        return conversor;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
