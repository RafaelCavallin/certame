package br.com.certame.desempenho;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ReconciliadorAgregados {
    private static final int TAMANHO_LOTE = 100;
    private final DesempenhoPendenciaRepository pendencias;
    private final AtualizadorAgregados atualizador;
    private final MeterRegistry metricas;

    public ReconciliadorAgregados(
            DesempenhoPendenciaRepository pendencias, AtualizadorAgregados atualizador) {
        this(pendencias, atualizador, Metrics.globalRegistry);
    }

    @Autowired
    public ReconciliadorAgregados(
            DesempenhoPendenciaRepository pendencias,
            AtualizadorAgregados atualizador,
            MeterRegistry metricas) {
        this.pendencias = pendencias;
        this.atualizador = atualizador;
        this.metricas = metricas;
        metricas.gauge("certame.desempenho.pendencia.fila", pendencias, repositorio -> repositorio.count());
    }

    @Scheduled(fixedDelayString = "900000")
    public void reconciliar() {
        List<DesempenhoPendencia> lote =
                pendencias.findAllByOrderByMarcadoEmAsc().stream().limit(TAMANHO_LOTE).toList();
        lote.forEach(this::reconstruir);
    }

    private void reconstruir(DesempenhoPendencia pendencia) {
        try {
            atualizador.atualizar(pendencia.getUsuarioId());
            pendencias.deleteById(pendencia.getUsuarioId());
            metricas.counter("certame.desempenho.agregado.atualizado", "origem", "reconciliacao").increment();
        } catch (RuntimeException falha) {
            metricas.counter("certame.desempenho.agregado.divergencia").increment();
        }
    }
}
