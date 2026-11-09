package br.com.zela.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades do Zela (prefixo {@code zela} no application.yml).
 * A aplicação não sobe se alguma obrigatória estiver ausente, por exemplo JWT_SECRET em produção.
 */
@Validated
@ConfigurationProperties(prefix = "zela")
public record ZelaProperties(
        @NotBlank String ambiente,
        @NotBlank String mensagem,
        @Valid Jwt jwt,
        @Valid Cors cors) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET precisa ter pelo menos 32 caracteres") String secret,
            @Positive int expiracaoHoras) {
    }

    /** Origens liberadas para chamadas do navegador (app Expo na web). */
    public record Cors(@NotEmpty List<String> origens) {
    }
}
