package br.com.zela.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @Valid Cors cors,
        @Valid Regras regras) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET precisa ter pelo menos 32 caracteres") String secret,
            @Positive int expiracaoHoras) {
    }

    /** Origens liberadas para chamadas do navegador (app Expo na web). */
    public record Cors(@NotEmpty List<String> origens) {
    }

    /**
     * Parâmetros das regras do fluxo, ajustáveis sem mudar código.
     *
     * @param confirmacoesParaConfirmar "sim" dos vizinhos para a ocorrência ficar CONFIRMADA
     * @param negacoesParaDescartar     "não" dos vizinhos para a ocorrência ser DESCARTADA
     * @param prazoUrgenciaHoras        prazo da OS de plantão no caminho de urgência
     * @param raioPadraoMetros          raio das buscas por proximidade quando o app não informa
     * @param raioCheckinMetros         distância máxima do local para o check-in do executor
     * @param horasSemConfirmacao       sem resposta dos vizinhos nesse tempo, a ocorrência segue sozinha
     * @param horasAceiteTacito         OS concluída sem contestação nesse tempo pode ser aceita tacitamente
     * @param confiancaAceiteTacito     confiança mínima da IA (0 a 1) para o aceite tácito
     */
    public record Regras(
            @Positive int confirmacoesParaConfirmar,
            @Positive int negacoesParaDescartar,
            @Positive int prazoUrgenciaHoras,
            @Positive int raioPadraoMetros,
            @Positive int raioCheckinMetros,
            @Positive int horasSemConfirmacao,
            @Positive int horasAceiteTacito,
            @DecimalMin("0") @DecimalMax("1") double confiancaAceiteTacito) {
    }
}
