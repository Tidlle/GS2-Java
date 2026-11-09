package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edição pelo autor, permitida enquanto a ocorrência não virou OS. */
public record OcorrenciaEdicao(
        @Schema(example = "1") @NotNull(message = "Informe a categoria") Long categoriaId,

        @NotBlank(message = "Informe a descrição")
        @Size(max = 1000, message = "Descrição com no máximo 1000 caracteres") String descricao,

        @Size(max = 200, message = "Endereço com no máximo 200 caracteres") String endereco) {
}
