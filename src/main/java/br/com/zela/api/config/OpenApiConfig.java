package br.com.zela.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI (Swagger UI em /swagger-ui.html).
 * O esquema "bearerAuth" permite colar o JWT no botão Authorize.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    OpenAPI zelaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Zela API")
                        .version("1.0.0")
                        .description("""
                                Zeladoria colaborativa com IA. O cidadão reporta um problema com foto, \
                                a IA classifica, os vizinhos confirmam e a prefeitura acompanha a ordem de \
                                serviço até a solução. GS 2026/2 · CitizenTech · FIAP.""")
                        .contact(new Contact().name("Grupo Zela")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /auth/login")));
    }
}
