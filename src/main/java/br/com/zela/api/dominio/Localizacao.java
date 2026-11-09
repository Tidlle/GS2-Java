package br.com.zela.api.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Ponto geográfico (graus decimais). Reaproveitado no local da ocorrência e no check-in da OS. */
@Embeddable
public record Localizacao(
        @Column(name = "latitude") Double latitude,
        @Column(name = "longitude") Double longitude) {

    private static final double RAIO_TERRA_M = 6_371_000;

    /** Distância em metros até outro ponto (fórmula de Haversine). */
    public double distanciaMetros(Localizacao outra) {
        double dLat = Math.toRadians(outra.latitude - latitude);
        double dLng = Math.toRadians(outra.longitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(outra.latitude))
                * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * RAIO_TERRA_M * Math.asin(Math.sqrt(a));
    }
}
