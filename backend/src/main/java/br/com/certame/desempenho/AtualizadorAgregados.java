package br.com.certame.desempenho;

import br.com.certame.desempenho.FonteRespostas.TotalPorAssunto;
import br.com.certame.desempenho.FonteRespostas.TotalPorChave;
import br.com.certame.desempenho.FonteRespostas.TotalPorDisciplina;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AtualizadorAgregados {
    private final FonteRespostas fonte;
    private final DesempenhoSemanalRepository semanais;
    private final DesempenhoDisciplinaRepository disciplinas;
    private final DesempenhoAssuntoRepository assuntos;
    private final MeterRegistry metricas;

    @Autowired
    public AtualizadorAgregados(FonteRespostas fonte, DesempenhoSemanalRepository semanais,
            DesempenhoDisciplinaRepository disciplinas, DesempenhoAssuntoRepository assuntos) {
        this(fonte, semanais, disciplinas, assuntos, Metrics.globalRegistry);
    }

    public AtualizadorAgregados(FonteRespostas fonte, DesempenhoSemanalRepository semanais,
            DesempenhoDisciplinaRepository disciplinas, DesempenhoAssuntoRepository assuntos, MeterRegistry metricas) {
        this.fonte = fonte; this.semanais = semanais; this.disciplinas = disciplinas; this.assuntos = assuntos; this.metricas = metricas;
    }

    /**
     * REQUIRES_NEW: chamado a partir de {@link OuvinteSessaoFinalizada}, um
     * {@code @TransactionalEventListener(AFTER_COMMIT)} — a transação da finalização já terminou
     * quando este método roda. Com a propagação padrão (REQUIRED), o Spring tenta "participar" da
     * sincronização ainda associada à thread e o Hibernate recusa o `delete` em massa com "No active
     * transaction for update or delete query"; REQUIRES_NEW força uma transação nova e genuína.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizar(UUID usuarioId) {
        var inicio = Timer.start(metricas);
        try {
            semanais.excluirDoUsuario(usuarioId); disciplinas.excluirDoUsuario(usuarioId); assuntos.excluirDoUsuario(usuarioId);
            fonte.porSemana(usuarioId).forEach(total -> salvarSemana(usuarioId, total));
            fonte.porDisciplina(usuarioId).forEach(total -> salvarDisciplina(usuarioId, total));
            fonte.porAssunto(usuarioId).forEach(total -> salvarAssunto(usuarioId, total));
            metricas.counter("certame.desempenho.agregado.atualizado", "origem", "incremental").increment();
        } finally {
            inicio.stop(Timer.builder("certame.desempenho.agregado.atualizacao").tag("origem", "incremental").register(metricas));
        }
    }

    private void salvarSemana(UUID usuarioId, TotalPorChave<LocalDate> total) {
        semanais.upsert(usuarioId, total.chave(), total.volume(), total.acertos(), percentual(total));
    }

    private void salvarDisciplina(UUID usuarioId, TotalPorDisciplina total) {
        disciplinas.upsert(usuarioId, total.disciplinaId(), total.volume(), total.acertos(), percentual(total.volume(), total.acertos()));
    }

    private void salvarAssunto(UUID usuarioId, TotalPorAssunto total) {
        assuntos.upsert(usuarioId, total.assuntoId(), total.disciplinaId(), total.volume(), total.acertos(), percentual(total.volume(), total.acertos()));
    }

    private short percentual(TotalPorChave<LocalDate> total) { return percentual(total.volume(), total.acertos()); }

    private short percentual(int volume, int acertos) { return (short) Math.round(acertos * 100.0 / volume); }
}
