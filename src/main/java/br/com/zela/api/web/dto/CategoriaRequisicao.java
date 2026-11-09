package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Criação e edição de categoria (somente GESTOR). */
public record CategoriaRequisicao(
        @Schema(example = "Sinalização")
        @NotBlank(message = "Informe o nome") @Size(max = 60, message = "Nome com no máximo 60 caracteres") String nome,

        @Schema(example = "warning-outline", description = "Nome do ícone (Ionicons) exibido no app")
        @NotBlank(message = "Informe o ícone") @Size(max = 40, message = "Ícone com no máximo 40 caracteres") String icone,

        @Schema(example = "2")
        @NotNull(message = "Informe o executor responsável") Long executorResponsavelId,

        @Schema(example = "72", description = "Prazo contratual em horas")
        @NotNull(message = "Informe o prazo em horas")
        @Min(value = 1, message = "O prazo deve ser de pelo menos 1 hora")
        @Max(value = 720, message = "O prazo deve ser de no máximo 720 horas (30 dias)") Integer prazoHoras,

        @Schema(example = "true", description = "true: OS automática; false: fila de exceções do gestor")
        @NotNull(message = "Informe se a emissão é automática") Boolean emissaoAutomatica,

        @Schema(example = "2", description = "Risco de 1 (baixo) a 5 (risco à vida)")
        @NotNull(message = "Informe o risco base")
        @Min(value = 1, message = "O risco base vai de 1 a 5")
        @Max(value = 5, message = "O risco base vai de 1 a 5") Integer riscoBase) {
}
