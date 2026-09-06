package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ProgressoCadernoTest {
    @Test void TU15_calculaCoberturaParcial() {
        assertThat(ProgressoCaderno.percentual(18, 40)).isEqualTo(45);
    }

    @Test void TU16_devolveZeroParaCadernoVazio() {
        assertThat(ProgressoCaderno.percentual(0, 0)).isZero();
    }

    @Test void TU17_devolveCemParaTodasRespondidas() {
        assertThat(ProgressoCaderno.percentual(40, 40)).isEqualTo(100);
    }
}
