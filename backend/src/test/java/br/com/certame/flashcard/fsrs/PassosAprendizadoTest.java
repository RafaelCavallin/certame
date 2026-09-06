package br.com.certame.flashcard.fsrs;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.NotaRevisao;
import org.junit.jupiter.api.Test;

class PassosAprendizadoTest {

    @Test void TU12_levaCardNovoComBomAoSegundoPasso() {
        var transicao = PassosAprendizado.proximoEstado(EstadoFlashcard.NOVO, 0, NotaRevisao.BOM);

        assertThat(transicao.estado()).isEqualTo(EstadoFlashcard.APRENDENDO);
        assertThat(transicao.passo()).isEqualTo(1);
        assertThat(transicao.atrasoMinutos()).isEqualTo(PesosFsrs.PASSOS_APRENDIZADO_MINUTOS[1]);
    }

    @Test void TU13_graduaComBomNoUltimoPasso() {
        var ultimoPasso = PesosFsrs.PASSOS_APRENDIZADO_MINUTOS.length - 1;

        var transicao = PassosAprendizado.proximoEstado(EstadoFlashcard.APRENDENDO, ultimoPasso, NotaRevisao.BOM);

        assertThat(transicao.estado()).isEqualTo(EstadoFlashcard.REVISAO);
        assertThat(transicao.atrasoMinutos()).isNull();
    }

    @Test void TU14_graduaImediatamenteComFacil() {
        var transicao = PassosAprendizado.proximoEstado(EstadoFlashcard.APRENDENDO, 0, NotaRevisao.FACIL);

        assertThat(transicao.estado()).isEqualTo(EstadoFlashcard.REVISAO);
        assertThat(transicao.passo()).isEqualTo(0);
        assertThat(transicao.atrasoMinutos()).isNull();
    }
}
