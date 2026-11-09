package br.com.zela.api.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de acesso HTTP. API stateless: sem sessão e sem CSRF (o cliente envia o JWT a cada chamada).
 * A validação do JWT e as regras por papel entram na Fase 3.
 */
@Configuration
public class SecurityConfig {

    /** Rotas abertas: documentação, health check, informações da API e console do H2 (só existe em dev). */
    static final String[] ROTAS_PUBLICAS = {
            "/info",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    @Bean
    SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // O console do H2 usa frames da mesma origem.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
