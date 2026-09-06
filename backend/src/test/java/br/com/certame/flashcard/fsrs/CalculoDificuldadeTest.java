package br.com.certame.flashcard.fsrs;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.flashcard.NotaRevisao;
import org.junit.jupiter.api.Test;

class CalculoDificuldadeTest {

    @Test void TU05_inicialCobreAsQuatroNotasDentroDaFaixa() {
        var errei = CalculoDificuldade.inicial(NotaRevisao.ERREI);
        var dificil = CalculoDificuldade.inicial(NotaRevisao.DIFICIL);
        var bom = CalculoDificuldade.inicial(NotaRevisao.BOM);
        var facil = CalculoDificuldade.inicial(NotaRevisao.FACIL);

        assertThat(errei).isBetween(1.0, 10.0);
        assertThat(dificil).isBetween(1.0, 10.0);
        assertThat(bom).isBetween(1.0, 10.0);
        assertThat(facil).isBetween(1.0, 10.0);
        assertThat(errei).isGreaterThan(dificil).isGreaterThan(bom).isGreaterThan(facil);
    }

    @Test void TU06_proximaAplicaReversaoAMediaEmD0Facil() {
        var dificuldadeAlta = 9.0;
        var dificuldadeBaixa = 1.5;

        var apoisBom = CalculoDificuldade.proxima(dificuldadeAlta, NotaRevisao.BOM);
        var apoisErrei = CalculoDificuldade.proxima(dificuldadeBaixa, NotaRevisao.ERREI);

        assertThat(apoisBom).isLessThan(dificuldadeAlta).isBetween(1.0, 10.0);
        assertThat(apoisErrei).isGreaterThan(dificuldadeBaixa).isBetween(1.0, 10.0);
    }

    @Test void TU07_saturaEmUmEEmDez() {
        var dificuldade = CalculoDificuldade.inicial(NotaRevisao.BOM);
        for (var i = 0; i < 10; i++) {
            dificuldade = CalculoDificuldade.proxima(dificuldade, NotaRevisao.FACIL);
        }
        var saturadaEmUm = dificuldade;

        dificuldade = CalculoDificuldade.inicial(NotaRevisao.BOM);
        for (var i = 0; i < 10; i++) {
            dificuldade = CalculoDificuldade.proxima(dificuldade, NotaRevisao.ERREI);
        }
        var saturadaEmDez = dificuldade;

        assertThat(saturadaEmUm).isBetween(1.0, 1.1);
        assertThat(saturadaEmDez).isBetween(9.7, 10.0);
    }
}
