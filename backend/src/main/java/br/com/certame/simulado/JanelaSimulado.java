package br.com.certame.simulado;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JanelaSimulado {
    private final Clock clock;

    public JanelaSimulado(Clock clock) {
        this.clock = clock;
    }

    public EstadoSimulado estado(StatusSimulado status, Instant abertura, Instant encerramento) {
        if (status == StatusSimulado.RASCUNHO) return EstadoSimulado.RASCUNHO;
        if (status == StatusSimulado.ENCERRADO) return EstadoSimulado.ENCERRADO;
        var agora = Instant.now(clock);
        if (abertura == null || agora.isBefore(abertura)) return EstadoSimulado.AGENDADO;
        if (encerramento == null || agora.isBefore(encerramento)) return EstadoSimulado.DISPONIVEL;
        return EstadoSimulado.ENCERRADO;
    }

    public boolean aceitaNovaTentativa(EstadoSimulado estado) {
        return estado == EstadoSimulado.DISPONIVEL;
    }
}
