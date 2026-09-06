package br.com.certame.desempenho;

import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.desempenho.dto.DesempenhoDisciplinaResumo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisciplinaDesempenhoService {
    private final DesempenhoDisciplinaRepository desempenhos;
    private final DisciplinaRepository disciplinas;
    private final MeterRegistry metricas;

    public DisciplinaDesempenhoService(DesempenhoDisciplinaRepository desempenhos,
                                       DisciplinaRepository disciplinas, MeterRegistry metricas) {
        this.desempenhos = desempenhos;
        this.disciplinas = disciplinas;
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public java.util.List<DesempenhoDisciplinaResumo> consultar(UUID usuarioId) {
        var inicio = Timer.start(metricas);
        try {
            return desempenhos.findByIdUsuarioIdOrderByVolumeDesc(usuarioId).stream()
                    .filter(desempenho -> desempenho.getVolume() > 0)
                    .map(this::resumir).toList();
        } finally {
            inicio.stop(timer("disciplinas"));
        }
    }

    private DesempenhoDisciplinaResumo resumir(DesempenhoDisciplina desempenho) {
        var disciplina = disciplinas.findById(desempenho.getDisciplinaId()).orElse(null);
        return new DesempenhoDisciplinaResumo(desempenho.getDisciplinaId(), disciplina == null ? null : disciplina.getNome(),
                desempenho.getVolume(), desempenho.getPctAcerto());
    }

    private Timer timer(String painel) {
        return Timer.builder("certame.desempenho.consulta").tag("painel", painel).register(metricas);
    }
}
