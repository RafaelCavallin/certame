package br.com.certame.estudo.caderno;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CadernoObservabilidade {
    private final MeterRegistry metricas;

    public CadernoObservabilidade(MeterRegistry metricas) { this.metricas = metricas; }

    public void sessaoIniciada(ReferenciaCaderno referencia) {
        var tipo = referencia.automatico() ? "AUTOMATICO" : "MANUAL";
        Counter.builder("certame.caderno.sessao.iniciada").tag("tipo", tipo).register(metricas).increment();
    }

    public void progresso(int percentual) {
        DistributionSummary.builder("certame.caderno.progresso").register(metricas).record(percentual);
    }
}
