package br.com.certame.desempenho;

import br.com.certame.desempenho.dto.ResumoDesempenho;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumoDesempenhoService {
    private final DesempenhoDisciplinaRepository disciplinas;
    private final MeterRegistry metricas;

    public ResumoDesempenhoService(DesempenhoDisciplinaRepository disciplinas, MeterRegistry metricas) {
        this.disciplinas = disciplinas;
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResumoDesempenho consultar(UUID usuarioId) {
        var inicio = Timer.start(metricas);
        try {
            var totais = disciplinas.findByIdUsuarioIdOrderByVolumeDesc(usuarioId);
            return montar(totais);
        } finally {
            inicio.stop(timer("resumo"));
        }
    }

    private ResumoDesempenho montar(java.util.List<DesempenhoDisciplina> totais) {
        var volume = totais.stream().mapToInt(DesempenhoDisciplina::getVolume).sum();
        var acertos = totais.stream().mapToInt(DesempenhoDisciplina::getAcertos).sum();
        var percentual = volume == 0 ? null : percentual(volume, acertos);
        return new ResumoDesempenho(volume, acertos, volume - acertos, percentual, volume > 0);
    }

    private Integer percentual(int volume, int acertos) { return (int) Math.round(acertos * 100.0 / volume); }

    private Timer timer(String painel) {
        return Timer.builder("certame.desempenho.consulta").tag("painel", painel).register(metricas);
    }
}
