package br.com.zela.api.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Posição do GPS do executor no momento do check-in. */
public record CheckinRequisicao(
        @Schema(example = "-23.5884")
        @NotNull(message = "Informe a latitude")
        @DecimalMin(value = "-90", message = "Latitude entre -90 e 90")
        @DecimalMax(value = "90", message = "Latitude entre -90 e 90") Double latitude,

        @Schema(example = "-46.6352")
        @NotNull(message = "Informe a longitude")
        @DecimalMin(value = "-180", message = "Longitude entre -180 e 180")
        @DecimalMax(value = "180", message = "Longitude entre -180 e 180") Double longitude) {
}
