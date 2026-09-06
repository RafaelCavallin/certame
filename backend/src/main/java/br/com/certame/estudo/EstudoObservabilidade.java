package br.com.certame.estudo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class EstudoObservabilidade {
    private final MeterRegistry metricas;

    public EstudoObservabilidade(MeterRegistry metricas, SessaoEstudoRepository sessoes) {
        this.metricas = metricas;
        io.micrometer.core.instrument.Gauge.builder("certame.estudo.sessoes_ativas", sessoes,
                SessaoEstudoRepository::countByFinalizadaEmIsNullAndExpiradaEmIsNull).register(metricas);
    }

    public Timer.Sample iniciar() { return Timer.start(metricas); }
    public void abertura(Timer.Sample inicio, ModoSessao modo, int tamanho, OrigemSessao origem) { parar(inicio, "certame.estudo.abertura", modo, tamanho, origem); }
    public void resposta(Timer.Sample inicio, ModoSessao modo) { inicio.stop(Timer.builder("certame.estudo.resposta").tag("modo", modo.name()).publishPercentiles(0.95, 0.99).register(metricas)); }
    public void finalizacao(Timer.Sample inicio, ModoSessao modo, int tamanho) { parar(inicio, "certame.estudo.finalizacao", modo, tamanho, null); }
    public void serieVazia(OrigemSessao origem) { Counter.builder("certame.estudo.serie_vazia").tag("origem", origem.name()).register(metricas).increment(); }
    public void sessaoExpirada(ModoSessao modo) { Counter.builder("certame.estudo.sessao_expirada").tag("modo", modo.name()).register(metricas).increment(); }
    private void parar(Timer.Sample inicio, String nome, ModoSessao modo, int tamanho, OrigemSessao origem) {
        var builder = Timer.builder(nome).tag("modo", modo.name()).tag("tamanho", Integer.toString(tamanho));
        if (origem != null) builder.tag("origem", origem.name());
        inicio.stop(builder.publishPercentiles(0.95, 0.99).register(metricas));
    }
}
