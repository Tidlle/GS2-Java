package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Encaminhamento a outro órgão, com o protocolo recebido de lá. */
public record EncaminhamentoRequisicao(
        @Schema(example = "DER-SP")
        @NotBlank(message = "Informe o órgão") @Size(max = 120, message = "Órgão com no máximo 120 caracteres") String orgao,

        @Schema(example = "DER-2026-005120")
        @NotBlank(message = "Informe o protocolo")
        @Size(max = 60, message = "Protocolo com no máximo 60 caracteres") String protocolo) {
}
