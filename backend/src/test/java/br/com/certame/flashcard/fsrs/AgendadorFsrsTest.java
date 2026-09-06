package br.com.certame.flashcard.fsrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.NotaRevisao;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AgendadorFsrsTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");
    private static final double TOLERANCIA = 1e-4;
    private final AgendadorFsrs agendador = new AgendadorFsrs();

    @Test void TU15_recalculaMemoriaEAgendaComBom() {
        var memoria = new EstadoMemoria(10.0, 6.0, EstadoFlashcard.REVISAO, 0);
        var contexto = new ContextoAgendamento(0.9, 365, 10, AGORA);

        var agendamento = agendador.proximo(memoria, NotaRevisao.BOM, contexto);

        assertThat(agendamento.estabilidade()).isNotEqualTo(memoria.estabilidade());
        assertThat(agendamento.dificuldade()).isNotEqualTo(memoria.dificuldade());
        var diasEsperados = (int) Math
                .round(CurvaEsquecimento.intervaloParaRetencao(agendamento.estabilidade(), 0.9));
        assertThat(agendamento.intervaloDias()).isEqualTo(diasEsperados);
        assertThat(agendamento.proximaRevisaoEm()).isEqualTo(AGORA.plus(Duration.ofDays(diasEsperados)));
    }

    @Test void TU16_comErreiEmCardDeRevisao() {
        var memoria = new EstadoMemoria(10.0, 6.0, EstadoFlashcard.REVISAO, 0);
        var contexto = new ContextoAgendamento(0.9, 365, 10, AGORA);

        var agendamento = agendador.proximo(memoria, NotaRevisao.ERREI, contexto);

        assertThat(agendamento.estabilidade()).isLessThanOrEqualTo(memoria.estabilidade());
        assertThat(agendamento.estado()).isEqualTo(EstadoFlashcard.REAPRENDENDO);
        assertThat(agendamento.passoAprendizado()).isZero();
        assertThat(agendamento.intervaloMinutos())
                .isEqualTo(PesosFsrs.PASSOS_REAPRENDIZADO_MINUTOS[0]);
    }

    @Test void TU17_truncaOIntervaloNoMaximoDoDeck() {
        var memoria = new EstadoMemoria(500.0, 5.0, EstadoFlashcard.REVISAO, 0);
        var contexto = new ContextoAgendamento(0.9, 30, 20, AGORA);

        var agendamento = agendador.proximo(memoria, NotaRevisao.BOM, contexto);

        assertThat(agendamento.intervaloDias()).isEqualTo(30);
    }

    @Test void TU18_respeitaRetencoesAlvoDiferentesNoMesmoCard() {
        var memoria = new EstadoMemoria(10.0, 5.0, EstadoFlashcard.REVISAO, 0);
        var contexto90 = new ContextoAgendamento(0.90, 365, 5, AGORA);
        var contexto95 = new ContextoAgendamento(0.95, 365, 5, AGORA);

        var agendamento90 = agendador.proximo(memoria, NotaRevisao.BOM, contexto90);
        var agendamento95 = agendador.proximo(memoria, NotaRevisao.BOM, contexto95);

        assertThat(agendamento90.estabilidade()).isEqualTo(agendamento95.estabilidade());
        assertThat(agendamento90.dificuldade()).isEqualTo(agendamento95.dificuldade());
        assertThat(agendamento90.intervaloDias()).isNotEqualTo(agendamento95.intervaloDias());
    }

    @Test void TU19_previsoesCoincideComProximoNasQuatroNotas() {
        var memoria = new EstadoMemoria(8.0, 4.5, EstadoFlashcard.REVISAO, 0);
        var contexto = new ContextoAgendamento(0.9, 365, 3, AGORA);

        var previsoes = agendador.previsoes(memoria, contexto);

        for (var nota : NotaRevisao.values()) {
            var esperado = agendador.proximo(memoria, nota, contexto).intervaloMinutos();
            assertThat(previsoes.minutos(nota)).isEqualTo(esperado);
        }
    }

    @Test void TU20_naoMutaOEstadoMemoriaRecebido() {
        var memoria = new EstadoMemoria(8.0, 4.5, EstadoFlashcard.REVISAO, 0);
        var contexto = new ContextoAgendamento(0.9, 365, 3, AGORA);

        for (var i = 0; i < 20; i++) {
            agendador.proximo(memoria, NotaRevisao.BOM, contexto);
        }

        assertThat(memoria.estabilidade()).isEqualTo(8.0);
        assertThat(memoria.dificuldade()).isEqualTo(4.5);
        assertThat(memoria.estado()).isEqualTo(EstadoFlashcard.REVISAO);
        assertThat(memoria.passoAprendizado()).isZero();
    }

    @Test void TU21_reproduzATabelaDeCasosDeReferenciaDoFsrs5() {
        var contexto0Dias = new ContextoAgendamento(0.9, 36500, 0, AGORA);

        var passo1 = agendador.proximo(new EstadoMemoria(0.0, 0.0, EstadoFlashcard.NOVO, 0),
                NotaRevisao.BOM, contexto0Dias);
        assertPasso(passo1, 3.173000, 5.282434, EstadoFlashcard.APRENDENDO, 1, 0, 10);

        var passo2 = agendador.proximo(paraMemoria(passo1), NotaRevisao.BOM, contexto0Dias);
        assertPasso(passo2, 4.466858, 5.272968, EstadoFlashcard.REVISAO, 0, 4, 4 * 1440);

        var contexto4Dias = new ContextoAgendamento(0.9, 36500, passo2.intervaloDias(), AGORA);
        var passo3 = agendador.proximo(paraMemoria(passo2), NotaRevisao.BOM, contexto4Dias);
        assertPasso(passo3, 14.233327, 5.263545, EstadoFlashcard.REVISAO, 0, 14, 14 * 1440);

        var contexto14Dias = new ContextoAgendamento(0.9, 36500, passo3.intervaloDias(), AGORA);
        var passo4 = agendador.proximo(paraMemoria(passo3), NotaRevisao.ERREI, contexto14Dias);
        assertPasso(passo4, 2.436072, 6.784232, EstadoFlashcard.REAPRENDENDO, 0, 0, 10);

        var passo5 = agendador.proximo(paraMemoria(passo4), NotaRevisao.DIFICIL, contexto0Dias);
        assertPasso(passo5, 2.045914, 7.287269, EstadoFlashcard.REAPRENDENDO, 0, 0, 10);

        var passo6 = agendador.proximo(paraMemoria(passo5), NotaRevisao.FACIL, contexto0Dias);
        assertPasso(passo6, 4.827857, 6.830419, EstadoFlashcard.REVISAO, 0, 5, 5 * 1440);
    }

    private EstadoMemoria paraMemoria(Agendamento agendamento) {
        return new EstadoMemoria(agendamento.estabilidade(), agendamento.dificuldade(), agendamento.estado(),
                agendamento.passoAprendizado());
    }

    private void assertPasso(Agendamento agendamento, double estabilidade, double dificuldade,
            EstadoFlashcard estado, int passo, int intervaloDias, int intervaloMinutos) {
        assertThat(agendamento.estabilidade()).isCloseTo(estabilidade, offset(TOLERANCIA));
        assertThat(agendamento.dificuldade()).isCloseTo(dificuldade, offset(TOLERANCIA));
        assertThat(agendamento.estado()).isEqualTo(estado);
        assertThat(agendamento.passoAprendizado()).isEqualTo(passo);
        assertThat(agendamento.intervaloDias()).isEqualTo(intervaloDias);
        assertThat(agendamento.intervaloMinutos()).isEqualTo(intervaloMinutos);
    }
}
