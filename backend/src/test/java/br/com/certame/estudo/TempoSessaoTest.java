package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TempoSessaoTest {
    private static final Instant INICIO = Instant.parse("2026-08-31T12:00:00Z");

    @Test void TU31_descontaPausasDaDuracao() {
        var tempo = new TempoSessao(Clock.fixed(INICIO.plusSeconds(100), ZoneOffset.UTC));
        assertThat(tempo.duracaoSegundos(INICIO, null, 15_000)).isEqualTo(85);
    }

    @Test void TU32_ignoraPausaMenorQueValorJaGravado() {
        var tempo = new TempoSessao(Clock.systemUTC());
        assertThat(tempo.acumular(20_000, 10_000)).isEqualTo(20_000);
    }

    @Test void TU33_nuncaDevolveDuracaoNegativa() {
        var tempo = new TempoSessao(Clock.systemUTC());
        assertThat(tempo.duracaoSegundos(INICIO, INICIO.minusSeconds(1), 0)).isZero();
    }

    @Test void TU34_congelaNaPausaMesmoComRelogioAvancando() {
        var tempo = new TempoSessao(Clock.fixed(INICIO.plusSeconds(500), java.time.ZoneOffset.UTC));
        var pausadaEm = INICIO.plusSeconds(100);
        assertThat(tempo.duracaoSegundos(INICIO, null, pausadaEm, 0)).isEqualTo(100);
    }

    @Test void TU35_semPausaUsaORelogioAtual() {
        var tempo = new TempoSessao(Clock.fixed(INICIO.plusSeconds(100), java.time.ZoneOffset.UTC));
        assertThat(tempo.duracaoSegundos(INICIO, null, null, 0)).isEqualTo(100);
    }
}
