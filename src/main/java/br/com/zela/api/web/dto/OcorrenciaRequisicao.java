package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Novo reporte do cidadão. A categoria e a descrição normalmente vêm da triagem da IA (editáveis). */
public record OcorrenciaRequisicao(
        @Schema(example = "1")
        @NotNull(message = "Informe a categoria") Long categoriaId,

        @Schema(example = "Poste de iluminação pública apagado, deixando o trecho escuro à noite.")
        @NotBlank(message = "Informe a descrição")
        @Size(max = 1000, message = "Descrição com no máximo 1000 caracteres") String descricao,

        @Schema(example = "R. Domingos de Morais, 1500", description = "Opcional: sem endereço, usa a localização do GPS")
        @Size(max = 200, message = "Endereço com no máximo 200 caracteres") String endereco,

        @Schema(example = "-23.5889")
        @NotNull(message = "Informe a latitude")
        @DecimalMin(value = "-90", message = "Latitude entre -90 e 90")
        @DecimalMax(value = "90", message = "Latitude entre -90 e 90") Double latitude,

        @Schema(example = "-46.6349")
        @NotNull(message = "Informe a longitude")
        @DecimalMin(value = "-180", message = "Longitude entre -180 e 180")
        @DecimalMax(value = "180", message = "Longitude entre -180 e 180") Double longitude,

        @Schema(description = "URL da foto do antes (devolvida pelo upload de fotos)")
        @Size(max = 300, message = "URL da foto com no máximo 300 caracteres") String fotoAntesUrl,

        @Schema(example = "false", description = "Risco à vida: pula a confirmação e vai ao plantão")
        Boolean urgente) {

    public boolean isUrgente() {
        return Boolean.TRUE.equals(urgente);
    }
}
