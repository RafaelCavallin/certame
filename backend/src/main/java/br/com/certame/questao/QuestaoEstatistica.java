package br.com.certame.questao;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "questao_estatistica")
public class QuestaoEstatistica {
    @Id @Column(name = "questao_id") private UUID questaoId;
    @OneToOne(fetch = FetchType.LAZY) @MapsId @JoinColumn(name = "questao_id") private Questao questao;
    @Column(name = "total_respostas", nullable = false) private int totalRespostas;
    @Column(name = "total_acertos", nullable = false) private int totalAcertos;
    @Column(name = "pct_acerto") private Short pctAcerto;
    @Column(name = "tempo_medio_ms") private Integer tempoMedioMs;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected QuestaoEstatistica() {}
    public QuestaoEstatistica(Questao questao) { this.questao = questao; }
    @PrePersist void aoCriar() { if (atualizadoEm == null) atualizadoEm = Instant.now(); }
    public UUID getQuestaoId() { return questaoId; }
    public Questao getQuestao() { return questao; }
    public int getTotalRespostas() { return totalRespostas; }
    public int getTotalAcertos() { return totalAcertos; }
    public Short getPctAcerto() { return pctAcerto; }
    public Integer getTempoMedioMs() { return tempoMedioMs; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void atualizar(int respostas, int acertos, Short percentual, Integer tempoMedio, Instant instante) { totalRespostas = respostas; totalAcertos = acertos; pctAcerto = percentual; tempoMedioMs = tempoMedio; atualizadoEm = instante; }
}
