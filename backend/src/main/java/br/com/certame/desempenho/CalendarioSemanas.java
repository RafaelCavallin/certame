package br.com.certame.desempenho;

import br.com.certame.comum.ZonaAplicacao;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class CalendarioSemanas {
    private static final int QUANTIDADE_SEMANAS = 8;
    private static final DateTimeFormatter ROTULO = DateTimeFormatter.ofPattern("dd/MM");
    private final Clock clock;

    public CalendarioSemanas(Clock clock) { this.clock = clock; }

    public List<LocalDate> ultimasOitoSemanas() {
        var atual = ZonedDateTime.now(clock).withZoneSameInstant(ZonaAplicacao.AMERICA_SAO_PAULO)
                .toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return IntStream.range(0, QUANTIDADE_SEMANAS)
                .mapToObj(indice -> atual.minusWeeks(QUANTIDADE_SEMANAS - 1L - indice))
                .toList();
    }

    public List<SemanaEvolucao> casar(List<LocalDate> semanas, Map<LocalDate, TotalSemana> agregados) {
        return semanas.stream().map(semana -> montarSemana(semana, agregados.get(semana))).toList();
    }

    private SemanaEvolucao montarSemana(LocalDate semana, TotalSemana agregado) {
        if (agregado == null) {
            return new SemanaEvolucao(semana, ROTULO.format(semana), 0, null);
        }
        return new SemanaEvolucao(semana, ROTULO.format(semana), agregado.volume(), agregado.pctAcerto());
    }
}
