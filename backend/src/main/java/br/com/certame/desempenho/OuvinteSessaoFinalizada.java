package br.com.certame.desempenho;

import br.com.certame.comum.evento.SessaoFinalizada;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OuvinteSessaoFinalizada {
    private static final Logger LOG = LoggerFactory.getLogger(OuvinteSessaoFinalizada.class);
    private final AtualizadorAgregados atualizador;
    private final DesempenhoPendenciaRepository pendencias;
    private final MeterRegistry metricas;

    public OuvinteSessaoFinalizada(AtualizadorAgregados atualizador, DesempenhoPendenciaRepository pendencias) {
        this(atualizador, pendencias, Metrics.globalRegistry);
    }

    @Autowired
    public OuvinteSessaoFinalizada(AtualizadorAgregados atualizador, DesempenhoPendenciaRepository pendencias, MeterRegistry metricas) {
        this.atualizador = atualizador; this.pendencias = pendencias; this.metricas = metricas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoFinalizar(SessaoFinalizada evento) {
        try { atualizador.atualizar(evento.usuarioId()); }
        // "certame.desempenho.pendencia.fila" já é o nome do gauge de ReconciliadorAgregados (tamanho
        // da fila); um contador com o mesmo nome faz o Micrometer recusar o registro.
        catch (RuntimeException falha) { pendencias.save(new DesempenhoPendencia(evento.usuarioId())); metricas.counter("certame.desempenho.pendencia.registrada").increment(); LOG.warn("Falha ao atualizar desempenho do usuario {}", evento.usuarioId(), falha); }
    }
}
