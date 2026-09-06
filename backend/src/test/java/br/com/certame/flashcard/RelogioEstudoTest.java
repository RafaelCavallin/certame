package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RelogioEstudoTest {

    @Test
    void TU53_resolveODiaNaZonaDoEstudo() {
        var meiaNoiteUtc = Instant.parse("2026-03-05T02:30:00Z");
        var relogio = new RelogioEstudo(Clock.fixed(meiaNoiteUtc, ZoneOffset.UTC), "America/Sao_Paulo");

        assertThat(relogio.hoje()).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThat(relogio.inicioDoDia()).isEqualTo(Instant.parse("2026-03-04T03:00:00Z"));
        assertThat(relogio.instante()).isEqualTo(meiaNoiteUtc);
    }
}
