package br.com.zela.api.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class LocalizacaoTest {

    @Test
    void mesmoPontoTemDistanciaZero() {
        Localizacao p = new Localizacao(-23.5893, -46.6347);
        assertThat(p.distanciaMetros(p)).isZero();
    }

    @Test
    void umMilesimoDeGrauDeLatitudeEhCercaDe111Metros() {
        Localizacao a = new Localizacao(-23.5893, -46.6347);
        Localizacao b = new Localizacao(-23.5903, -46.6347);
        assertThat(a.distanciaMetros(b)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void distanciaEhSimetrica() {
        Localizacao escola = new Localizacao(-23.5935, -46.6304);
        Localizacao poste = new Localizacao(-23.5884, -46.6352);
        assertThat(escola.distanciaMetros(poste)).isCloseTo(poste.distanciaMetros(escola), within(1e-6));
    }
}
