package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Conclusão do serviço: a foto do depois, tirada no local, é obrigatória. */
public record ConclusaoRequisicao(
        @Schema(description = "URL da foto do depois (devolvida pelo upload de fotos)")
        @NotBlank(message = "A foto do depois é obrigatória")
        @Size(max = 300, message = "URL da foto com no máximo 300 caracteres") String fotoDepoisUrl,

        @Schema(example = "Troca de reator e lâmpada.")
        @Size(max = 500, message = "Observação com no máximo 500 caracteres") String observacao) {
}
