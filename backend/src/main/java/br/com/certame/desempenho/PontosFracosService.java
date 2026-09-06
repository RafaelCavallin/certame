package br.com.certame.desempenho;

import br.com.certame.catalogo.AssuntoRepository;
import br.com.certame.desempenho.dto.PontoFracoResumo;
import br.com.certame.desempenho.dto.PontosFracos;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PontosFracosService {
    private final DesempenhoAssuntoRepository desempenhos;
    private final DesempenhoDisciplinaRepository disciplinas;
    private final AssuntoRepository assuntos;
    private final MeterRegistry metricas;

    public PontosFracosService(DesempenhoAssuntoRepository desempenhos, DesempenhoDisciplinaRepository disciplinas,
                               AssuntoRepository assuntos, MeterRegistry metricas) {
        this.desempenhos = desempenhos;
        this.disciplinas = disciplinas;
        this.assuntos = assuntos;
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PontosFracos consultar(UUID usuarioId) {
        var inicio = Timer.start(metricas);
        try {
            var media = media(usuarioId);
            var totais = desempenhos.findByIdUsuarioIdAndVolumeGreaterThanEqualOrderByVolumeDesc(usuarioId, SeletorPontosFracos.MINIMO_RESPOSTAS);
            var itens = new SeletorPontosFracos().selecionar(totais.stream().map(this::total).toList(), media).stream().map(this::dto).toList();
            return new PontosFracos(media, SeletorPontosFracos.MINIMO_RESPOSTAS, itens);
        } finally {
            inicio.stop(timer("pontos-fracos"));
        }
    }

    private Integer media(UUID usuarioId) {
        var totais = disciplinas.findByIdUsuarioIdOrderByVolumeDesc(usuarioId);
        var volume = totais.stream().mapToInt(DesempenhoDisciplina::getVolume).sum();
        var acertos = totais.stream().mapToInt(DesempenhoDisciplina::getAcertos).sum();
        return volume == 0 ? null : (int) Math.round(acertos * 100.0 / volume);
    }

    private TotalPorAssunto total(DesempenhoAssunto desempenho) {
        var assunto = assuntos.findById(desempenho.getAssuntoId()).orElse(null);
        var disciplina = assunto == null ? null : assunto.getDisciplina();
        return new TotalPorAssunto(desempenho.getAssuntoId(), assunto == null ? null : assunto.getNome(),
                desempenho.getDisciplinaId(), disciplina == null ? null : disciplina.getNome(),
                desempenho.getVolume(), desempenho.getPctAcerto());
    }

    private PontoFracoResumo dto(PontoFraco ponto) {
        return new PontoFracoResumo(ponto.assuntoId(), ponto.assunto(), ponto.disciplinaId(), ponto.disciplina(), ponto.volume(), ponto.pctAcerto());
    }

    private Timer timer(String painel) {
        return Timer.builder("certame.desempenho.consulta").tag("painel", painel).register(metricas);
    }
}
