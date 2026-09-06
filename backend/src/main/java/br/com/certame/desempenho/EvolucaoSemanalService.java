package br.com.certame.desempenho;

import br.com.certame.desempenho.dto.EvolucaoSemanal;
import br.com.certame.desempenho.dto.SemanaEvolucaoResumo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvolucaoSemanalService {
    private final DesempenhoSemanalRepository semanas;
    private final CalendarioSemanas calendario;
    private final MeterRegistry metricas;

    public EvolucaoSemanalService(DesempenhoSemanalRepository semanas, Clock clock, MeterRegistry metricas) {
        this.semanas = semanas;
        this.calendario = new CalendarioSemanas(clock);
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public EvolucaoSemanal consultar(UUID usuarioId) {
        var inicio = Timer.start(metricas);
        try {
            var chaves = calendario.ultimasOitoSemanas();
            var agregados = buscar(usuarioId, chaves);
            var serie = calendario.casar(chaves, agregados).stream().map(this::dto).toList();
            return new EvolucaoSemanal(serie, media(agregados), melhor(serie), serie.stream().mapToInt(s -> s.volume()).sum());
        } finally {
            inicio.stop(timer("evolucao"));
        }
    }

    private Map<LocalDate, TotalSemana> buscar(UUID usuarioId, java.util.List<LocalDate> chaves) {
        return semanas.findByIdUsuarioIdAndIdSemanaBetweenOrderByIdSemanaAsc(usuarioId, chaves.getFirst(), chaves.getLast())
                .stream().collect(Collectors.toMap(DesempenhoSemanal::getSemana,
                        semana -> new TotalSemana(semana.getVolume(), semana.getAcertos())));
    }

    private SemanaEvolucaoResumo dto(SemanaEvolucao semana) {
        return new SemanaEvolucaoResumo(semana.semana(), semana.rotulo(), semana.volume(), semana.pctAcerto());
    }

    private Integer media(Map<LocalDate, TotalSemana> agregados) {
        var volume = agregados.values().stream().mapToInt(TotalSemana::volume).sum();
        var acertos = agregados.values().stream().mapToInt(TotalSemana::acertos).sum();
        return volume == 0 ? null : (int) Math.round(acertos * 100.0 / volume);
    }

    private SemanaEvolucaoResumo melhor(java.util.List<SemanaEvolucaoResumo> serie) {
        return serie.stream().filter(semana -> semana.pctAcerto() != null)
                .max(java.util.Comparator.comparingInt(SemanaEvolucaoResumo::pctAcerto)).orElse(null);
    }

    private Timer timer(String painel) {
        return Timer.builder("certame.desempenho.consulta").tag("painel", painel).register(metricas);
    }
}
