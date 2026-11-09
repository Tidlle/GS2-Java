package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Resposta a "Esse problema ainda está aqui?". */
public record ConfirmacaoRequisicao(
        @Schema(example = "true") @NotNull(message = "Informe se o problema ainda existe") Boolean aindaExiste) {
}
