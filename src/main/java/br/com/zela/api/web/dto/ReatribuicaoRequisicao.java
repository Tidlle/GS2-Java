package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Troca o executor da OS (o prazo recomeça com o prazo da categoria). */
public record ReatribuicaoRequisicao(
        @Schema(example = "3") @NotNull(message = "Informe o novo executor") Long executorId) {
}
