package br.com.certame.flashcard.deck;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LimitesDeckTest {

    @Test
    void TU22_normalizarAjustaQuinhentasRevisoesPorDiaParaQuatrocentas() {
        var limites = new LimitesDeck((short) 10, (short) 500, (short) 180, new BigDecimal("0.90"));

        var normalizado = limites.normalizar();

        assertThat(normalizado.revisoesDia()).isEqualTo((short) 400);
    }

    @Test
    void TU23_normalizarAjustaCadaCampoForaDoIntervalo() {
        var limites = new LimitesDeck((short) -5, (short) 2, (short) 4000, new BigDecimal("0.99"));

        var normalizado = limites.normalizar();

        assertThat(normalizado.novosDia()).isEqualTo((short) 0);
        assertThat(normalizado.revisoesDia()).isEqualTo((short) 5);
        assertThat(normalizado.intervaloMaxDias()).isEqualTo((short) 3650);
        assertThat(normalizado.retencaoAlvo()).isEqualByComparingTo("0.95");
    }

    @Test
    void TU24_padraoTrazOsLimitesDeSemDeck() {
        assertThat(LimitesDeck.PADRAO.novosDia()).isEqualTo((short) 10);
        assertThat(LimitesDeck.PADRAO.revisoesDia()).isEqualTo((short) 60);
        assertThat(LimitesDeck.PADRAO.intervaloMaxDias()).isEqualTo((short) 180);
        assertThat(LimitesDeck.PADRAO.retencaoAlvo()).isEqualByComparingTo("0.90");
    }
}
