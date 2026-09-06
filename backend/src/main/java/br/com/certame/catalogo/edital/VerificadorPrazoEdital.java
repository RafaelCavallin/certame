package br.com.certame.catalogo.edital;

import br.com.certame.comum.ZonaAplicacao;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class VerificadorPrazoEdital {
    private final Clock clock;

    public VerificadorPrazoEdital(Clock clock) { this.clock = clock; }

    public boolean vencido(StatusEdital status, LocalDate prazo) {
        if (status != StatusEdital.ABERTO) return false;
        if (prazo == null) return false;
        return prazo.isBefore(LocalDate.now(clock.withZone(ZonaAplicacao.AMERICA_SAO_PAULO)));
    }
}
