package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Emissão manual pelo gestor. Campos vazios usam o executor e o prazo do contrato da categoria. */
public record EmissaoOSRequisicao(
        @Schema(example = "4", description = "Executor escolhido; vazio = executor sugerido da categoria") Long executorId,

        @Schema(example = "120", description = "Prazo em horas; vazio = prazo contratual da categoria")
        @Min(value = 1, message = "O prazo deve ser de pelo menos 1 hora")
        @Max(value = 720, message = "O prazo deve ser de no máximo 720 horas") Integer prazoHoras) {
}
