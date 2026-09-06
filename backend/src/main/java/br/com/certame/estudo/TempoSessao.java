package br.com.certame.estudo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class TempoSessao {
    private static final int MILIS_POR_SEGUNDO = 1000;
    private final Clock clock;

    public TempoSessao(Clock clock) { this.clock = clock; }

    public int duracaoSegundos(Instant inicio, Instant fim, int pausadoMs) {
        return duracaoSegundos(inicio, fim, null, pausadoMs);
    }

    /** Se a sessão está pausada (fim == null e pausadaEm != null), o relógio para em pausadaEm. */
    public int duracaoSegundos(Instant inicio, Instant fim, Instant pausadaEm, int pausadoMs) {
        var instanteFim = fim != null ? fim : (pausadaEm != null ? pausadaEm : clock.instant());
        var decorridoMs = Duration.between(inicio, instanteFim).toMillis();
        var ativoMs = Math.max(0L, decorridoMs - Math.max(0L, pausadoMs));
        return (int) Math.min(Integer.MAX_VALUE, ativoMs / MILIS_POR_SEGUNDO);
    }

    public int acumular(int pausadoAtual, int pausadoRecebido) {
        return Math.max(Math.max(0, pausadoAtual), Math.max(0, pausadoRecebido));
    }
}
