package br.com.certame.questao;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Observabilidade do fluxo de publicação de questões: contadores por transição e gauge por disciplina. */
@Component
public class MetricasPublicacao {
    private final QuestaoRepository questoes;
    private final MeterRegistry metricas;
    private final Set<UUID> gaugesRegistrados = ConcurrentHashMap.newKeySet();

    @Autowired
    public MetricasPublicacao(QuestaoRepository questoes) {
        this(questoes, Metrics.globalRegistry);
    }

    public MetricasPublicacao(QuestaoRepository questoes, MeterRegistry metricas) {
        this.questoes = questoes;
        this.metricas = metricas;
    }

    public void registrarRecusa(Map<String, String> pendencias) {
        pendencias.keySet().forEach(pendencia ->
                metricas.counter("certame.questao.publicacao.recusada", "pendencia", pendencia).increment());
    }

    public void registrarPublicada(Questao questao) {
        registrarGauge(questao);
        metricas.counter("certame.questao.publicada", "perfil", perfilAtor(), "disciplina", disciplina(questao)).increment();
    }

    public void registrarDespublicada() {
        metricas.counter("certame.questao.despublicada").increment();
    }

    private void registrarGauge(Questao questao) {
        if (questao.getDisciplina() == null) return;
        var disciplinaId = questao.getDisciplina().getId();
        if (disciplinaId == null || !gaugesRegistrados.add(disciplinaId)) return;
        Gauge.builder("certame.acervo.publicadas", questoes, repositorio ->
                        repositorio.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(disciplinaId, StatusQuestao.PUBLICADA))
                .tag("disciplina", disciplina(questao)).register(metricas);
    }

    private String disciplina(Questao questao) {
        return questao.getDisciplina() == null ? "sem_disciplina" : questao.getDisciplina().getNome();
    }

    private String perfilAtor() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null) return "desconhecido";
        return autenticacao.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("desconhecido");
    }
}
