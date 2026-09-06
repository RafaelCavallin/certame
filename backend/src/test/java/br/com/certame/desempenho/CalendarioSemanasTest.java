package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.certame.comum.ZonaAplicacao;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalendarioSemanasTest {
    private static final Clock DOMINGO = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZonaAplicacao.AMERICA_SAO_PAULO);

    @Test void TU01_geraOitoSegundasFeiras() {
        var semanas = new CalendarioSemanas(DOMINGO).ultimasOitoSemanas();
        assertThat(semanas).hasSize(8).containsExactly(LocalDate.parse("2026-07-06"), LocalDate.parse("2026-07-13"),
                LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-27"), LocalDate.parse("2026-08-03"),
                LocalDate.parse("2026-08-10"), LocalDate.parse("2026-08-17"), LocalDate.parse("2026-08-24"));
    }

    @Test void TU02_casaAgregadosERepresentaLacunas() {
        var semanas = new CalendarioSemanas(DOMINGO).ultimasOitoSemanas();
        var resultado = new CalendarioSemanas(DOMINGO).casar(semanas, Map.of(semanas.get(0), new TotalSemana(10, 7)));
        assertThat(resultado.get(0).pctAcerto()).isEqualTo(70);
        assertThat(resultado.get(1).volume()).isZero();
        assertThat(resultado.get(1).pctAcerto()).isNull();
    }

    @Test void TU03_mantemOitoItensComAtividadeAtual() {
        var calendario = new CalendarioSemanas(DOMINGO);
        var semanas = calendario.ultimasOitoSemanas();
        var resultado = calendario.casar(semanas, Map.of(semanas.get(7), new TotalSemana(5, 4)));
        assertThat(resultado).hasSize(8);
        assertThat(resultado.stream().filter(semana -> semana.pctAcerto() == null)).hasSize(7);
        assertThat(resultado.get(7).pctAcerto()).isEqualTo(80);
    }

    @Test void TU04_atravessaViradaDeAnoSemPularSemana() {
        var semanas = new CalendarioSemanas(Clock.fixed(Instant.parse("2027-01-03T12:00:00Z"), ZoneOffset.UTC)).ultimasOitoSemanas();
        assertThat(semanas).containsExactly(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-16"),
                LocalDate.parse("2026-11-23"), LocalDate.parse("2026-11-30"), LocalDate.parse("2026-12-07"),
                LocalDate.parse("2026-12-14"), LocalDate.parse("2026-12-21"), LocalDate.parse("2026-12-28"));
    }

    @Test void TU05_usaFusoDaAplicacaoParaFecharSemana() {
        var instante = Instant.parse("2026-08-31T02:00:00Z");
        var semanas = new CalendarioSemanas(Clock.fixed(instante, ZoneOffset.UTC)).ultimasOitoSemanas();
        assertThat(semanas.get(7)).isEqualTo(LocalDate.parse("2026-08-24"));
    }
}
