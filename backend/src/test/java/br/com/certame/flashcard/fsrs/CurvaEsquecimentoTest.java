package br.com.certame.flashcard.fsrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

class CurvaEsquecimentoTest {

    @Test void TU01_devolveRetencaoUmComZeroDiasDecorridos() {
        var retencao = CurvaEsquecimento.retrievability(15.0, 0);

        assertThat(retencao).isEqualTo(1.0);
    }

    @Test void TU02_decresceMonotonicamenteComOsDias() {
        var estabilidade = 20.0;

        var apos1Dia = CurvaEsquecimento.retrievability(estabilidade, 1);
        var apos7Dias = CurvaEsquecimento.retrievability(estabilidade, 7);
        var apos30Dias = CurvaEsquecimento.retrievability(estabilidade, 30);

        assertThat(apos1Dia).isGreaterThan(apos7Dias).isLessThan(1.0);
        assertThat(apos7Dias).isGreaterThan(apos30Dias).isGreaterThan(0.0);
    }

    @Test void TU03_fechaOCicloIntervaloParaRetencao() {
        var estabilidade = 12.5;

        var intervalo = CurvaEsquecimento.intervaloParaRetencao(estabilidade, 0.9);
        var retencaoObtida = CurvaEsquecimento.retrievability(estabilidade, intervalo);

        assertThat(retencaoObtida).isCloseTo(0.9, offset(1e-6));
    }

    @Test void TU04_encurtaOIntervaloQuandoARetencaoAlvoSobe() {
        var estabilidade = 30.0;

        var intervalo95 = CurvaEsquecimento.intervaloParaRetencao(estabilidade, 0.95);
        var intervalo90 = CurvaEsquecimento.intervaloParaRetencao(estabilidade, 0.90);
        var intervalo80 = CurvaEsquecimento.intervaloParaRetencao(estabilidade, 0.80);

        assertThat(intervalo95).isLessThan(intervalo90).isLessThan(intervalo80);
    }
}
