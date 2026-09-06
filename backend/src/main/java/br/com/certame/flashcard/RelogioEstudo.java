package br.com.certame.flashcard;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RelogioEstudo {
    private final Clock clock;
    private final ZoneId zona;

    public RelogioEstudo(Clock clock, @Value("${certame.estudo.zona:America/Sao_Paulo}") String zona) {
        this.clock = clock;
        this.zona = ZoneId.of(zona);
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(zona));
    }

    public Instant inicioDoDia() {
        return hoje().atStartOfDay(zona).toInstant();
    }

    public Instant instante() {
        return clock.instant();
    }
}
