package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ações que exigem justificativa (descartar, recusar, reprovar, cancelar). */
public record JustificativaRequisicao(
        @Schema(example = "Duplicada da ocorrência 12")
        @NotBlank(message = "Informe a justificativa")
        @Size(max = 500, message = "Justificativa com no máximo 500 caracteres") String justificativa) {
}
