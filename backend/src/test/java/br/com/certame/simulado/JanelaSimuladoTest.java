package br.com.certame.simulado;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JanelaSimuladoTest {
    private static final Instant ABERTURA = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant ENCERRAMENTO = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void TU08_derivaAgendadoDisponivelEEncerradoDaJanela() {
        var antes = janelaEm("2026-08-31T23:59:59Z");
        var durante = janelaEm("2026-09-01T12:00:00Z");
        var depois = janelaEm("2026-09-02T00:00:00Z");

        assertThat(antes.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO)).isEqualTo(EstadoSimulado.AGENDADO);
        assertThat(durante.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO)).isEqualTo(EstadoSimulado.DISPONIVEL);
        assertThat(depois.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO)).isEqualTo(EstadoSimulado.ENCERRADO);
    }

    @Test
    void TU09_recusaInicioForaDaJanela() {
        var antes = janelaEm("2026-08-31T23:59:59Z");
        var durante = janelaEm("2026-09-01T12:00:00Z");
        var depois = janelaEm("2026-09-02T00:00:00Z");

        assertThat(antes.aceitaNovaTentativa(antes.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO))).isFalse();
        assertThat(durante.aceitaNovaTentativa(durante.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO))).isTrue();
        assertThat(depois.aceitaNovaTentativa(depois.estado(StatusSimulado.PUBLICADO, ABERTURA, ENCERRAMENTO))).isFalse();
    }

    private JanelaSimulado janelaEm(String instante) {
        return new JanelaSimulado(Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
    }
}
