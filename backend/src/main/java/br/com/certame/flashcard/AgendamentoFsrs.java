package br.com.certame.flashcard;

import br.com.certame.comum.ZonaAplicacao;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.time.LocalDate;

/** Estado de agendamento FSRS de um flashcard (RN-08): estabilidade, dificuldade e retenção alvo. */
@Embeddable
public class AgendamentoFsrs {

    @Column(nullable = false)
    private Float estabilidade = 0f;

    @Column(nullable = false)
    private Float dificuldade = 0f;

    @Column(name = "intervalo_dias", nullable = false)
    private Short intervaloDias = 0;

    @Column(nullable = false)
    private Short repeticoes = 0;

    @Column(nullable = false)
    private Short lapsos = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFlashcard estado = EstadoFlashcard.NOVO;

    @Column(name = "passo_aprendizado", nullable = false)
    private Short passoAprendizado = 0;

    @Column(name = "ultima_revisao")
    private Instant ultimaRevisao;

    @Column(name = "proxima_revisao", nullable = false)
    private LocalDate proximaRevisao = LocalDate.now();

    @Column(name = "proxima_revisao_em", nullable = false)
    private Instant proximaRevisaoEm = Instant.now();

    /** Único ponto que grava proxima_revisao_em e proxima_revisao, mantendo o check flashcard_agenda_coerente. */
    public void agendarPara(Instant proximaRevisaoEm) {
        this.proximaRevisaoEm = proximaRevisaoEm;
        this.proximaRevisao = LocalDate.ofInstant(proximaRevisaoEm, ZonaAplicacao.AMERICA_SAO_PAULO);
    }

    public Float getEstabilidade() { return estabilidade; }
    public Float getDificuldade() { return dificuldade; }
    public Short getIntervaloDias() { return intervaloDias; }
    public Short getRepeticoes() { return repeticoes; }
    public Short getLapsos() { return lapsos; }
    public EstadoFlashcard getEstado() { return estado; }
    public Short getPassoAprendizado() { return passoAprendizado; }
    public Instant getUltimaRevisao() { return ultimaRevisao; }
    public LocalDate getProximaRevisao() { return proximaRevisao; }
    public Instant getProximaRevisaoEm() { return proximaRevisaoEm; }

    public void setEstabilidade(Float estabilidade) { this.estabilidade = estabilidade; }
    public void setDificuldade(Float dificuldade) { this.dificuldade = dificuldade; }
    public void setIntervaloDias(Short intervaloDias) { this.intervaloDias = intervaloDias; }
    public void setRepeticoes(Short repeticoes) { this.repeticoes = repeticoes; }
    public void setLapsos(Short lapsos) { this.lapsos = lapsos; }
    public void setEstado(EstadoFlashcard estado) { this.estado = estado; }
    public void setPassoAprendizado(Short passoAprendizado) { this.passoAprendizado = passoAprendizado; }
    public void setUltimaRevisao(Instant ultimaRevisao) { this.ultimaRevisao = ultimaRevisao; }
}
