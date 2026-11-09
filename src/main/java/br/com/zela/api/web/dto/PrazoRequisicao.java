package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Novo prazo da etapa atual, em horas a partir de agora. */
public record PrazoRequisicao(
        @Schema(example = "12")
        @NotNull(message = "Informe o prazo em horas")
        @Min(value = 1, message = "O prazo deve ser de pelo menos 1 hora")
        @Max(value = 720, message = "O prazo deve ser de no máximo 720 horas") Integer horas) {
}
