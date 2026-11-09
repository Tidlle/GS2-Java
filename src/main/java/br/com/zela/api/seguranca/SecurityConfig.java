package br.com.zela.api.seguranca;

import br.com.zela.api.config.ZelaProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Regras de acesso HTTP.
 *
 * <ol>
 *   <li>O app faz login em POST /auth/login e recebe um JWT.</li>
 *   <li>Nas próximas chamadas envia {@code Authorization: Bearer <token>}.</li>
 *   <li>O Resource Server do Spring valida assinatura e validade e monta o usuário com o papel (ROLE_*).</li>
 *   <li>{@code @PreAuthorize} em cada endpoint confere o papel; os serviços conferem a posse do recurso.</li>
 * </ol>
 *
 * Sem sessão e sem CSRF, pois a API é stateless e o token não é enviado por cookie.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rotas abertas: login, cadastro, documentação, health check e informações da API. */
    static final String[] ROTAS_PUBLICAS = {
            "/auth/login",
            "/auth/cadastro",
            "/info",
            "/error",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    private final HandlerExceptionResolver resolvedorDeErros;
    private final ZelaProperties propriedades;

    public SecurityConfig(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedorDeErros,
            ZelaProperties propriedades) {
        this.resolvedorDeErros = resolvedorDeErros;
        this.propriedades = propriedades;
    }

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(cors()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // O console do H2 (só em dev) usa frames da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        // Fotos: leitura pública (nome aleatório); envio exige login.
                        .requestMatchers(HttpMethod.GET, "/fotos/*").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        // 401 (sem token ou token inválido) no mesmo formato JSON dos outros erros.
                        .authenticationEntryPoint((req, resp, ex) -> resolvedorDeErros.resolveException(req, resp, null, ex)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, resp, ex) -> resolvedorDeErros.resolveException(req, resp, null, ex))
                        .accessDeniedHandler((req, resp, ex) -> resolvedorDeErros.resolveException(req, resp, null, ex)))
                .build();
    }

    /** CORS para o app Expo na web. O token vai no cabeçalho (não em cookie), então não há credenciais. */
    private CorsConfigurationSource cors() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(propriedades.cors().origens());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }
}
