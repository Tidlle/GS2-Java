package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Resposta do cidadão sobre a solução: confirma ou contesta. */
public record AvaliacaoRequisicao(
        @Schema(example = "true", description = "true confirma a solução; false contesta (a OS volta ao executor)")
        @NotNull(message = "Informe se a solução foi aprovada") Boolean aprovada,

        @Schema(example = "5", description = "Nota de 1 a 5 para o serviço (opcional)")
        @Min(value = 1, message = "A nota vai de 1 a 5") @Max(value = 5, message = "A nota vai de 1 a 5") Integer nota,

        @Schema(example = "Ficou ótimo, obrigado!")
        @Size(max = 500, message = "Comentário com no máximo 500 caracteres") String comentario) {
}
