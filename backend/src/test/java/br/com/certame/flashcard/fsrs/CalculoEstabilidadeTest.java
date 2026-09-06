package br.com.certame.flashcard.fsrs;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.flashcard.NotaRevisao;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CalculoEstabilidadeTest {

    @Test void TU08_inicialUsaOPesoDaNota() {
        var errei = CalculoEstabilidade.inicial(NotaRevisao.ERREI);
        var dificil = CalculoEstabilidade.inicial(NotaRevisao.DIFICIL);
        var bom = CalculoEstabilidade.inicial(NotaRevisao.BOM);
        var facil = CalculoEstabilidade.inicial(NotaRevisao.FACIL);

        assertThat(bom).isEqualTo(PesosFsrs.W[2]);
        assertThat(errei).isEqualTo(PesosFsrs.W[0]);
        assertThat(dificil).isEqualTo(PesosFsrs.W[1]);
        assertThat(facil).isEqualTo(PesosFsrs.W[3]);
    }

    @Test void TU09_aposAcertoCresceMaisComRetencaoBaixa() {
        var estabilidade = 15.0;
        var dificuldade = 5.0;

        var comRetencaoBaixa = CalculoEstabilidade.aposAcerto(estabilidade, dificuldade, 0.7, NotaRevisao.BOM);
        var comRetencaoAlta = CalculoEstabilidade.aposAcerto(estabilidade, dificuldade, 0.95, NotaRevisao.BOM);

        assertThat(comRetencaoBaixa).isGreaterThan(comRetencaoAlta);
    }

    @Test void TU10_aposLapsoNuncaSuperaAEstabilidadeAnterior() {
        var aleatorio = new Random(42);

        for (var i = 0; i < 20; i++) {
            var estabilidade = 0.5 + aleatorio.nextDouble() * 100;
            var dificuldade = 1 + aleatorio.nextDouble() * 9;
            var retrievability = aleatorio.nextDouble();

            var resultado = CalculoEstabilidade.aposLapso(estabilidade, dificuldade, retrievability);

            assertThat(resultado).isLessThanOrEqualTo(estabilidade);
        }
    }

    @Test void TU11_curtoPrazoAplicaAFormulaDeMesmoDia() {
        var estabilidade = 10.0;

        var comFacil = CalculoEstabilidade.curtoPrazo(estabilidade, NotaRevisao.FACIL);
        var comErrei = CalculoEstabilidade.curtoPrazo(estabilidade, NotaRevisao.ERREI);

        assertThat(comFacil).isGreaterThan(estabilidade);
        assertThat(comErrei).isLessThan(estabilidade);
        assertThat(comFacil).isBetween(PesosFsrs.ESTABILIDADE_MIN, PesosFsrs.ESTABILIDADE_MAX);
        assertThat(comErrei).isBetween(PesosFsrs.ESTABILIDADE_MIN, PesosFsrs.ESTABILIDADE_MAX);
    }
}
