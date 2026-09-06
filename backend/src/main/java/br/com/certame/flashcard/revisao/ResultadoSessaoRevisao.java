package br.com.certame.flashcard.revisao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Contadores de resultado de uma sessão de revisão, preenchidos quando ela é finalizada. */
@Embeddable
public class ResultadoSessaoRevisao {

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @Column(nullable = false)
    private Short revisados = 0;

    @Column(nullable = false)
    private Short errei = 0;

    @Column(nullable = false)
    private Short dificil = 0;

    @Column(nullable = false)
    private Short bom = 0;

    @Column(nullable = false)
    private Short facil = 0;

    @Column(name = "tempo_ms", nullable = false)
    private Integer tempoMs = 0;

    public void finalizar(Instant finalizadaEm, short revisados, short errei, short dificil, short bom, short facil,
            int tempoMs) {
        this.finalizadaEm = finalizadaEm;
        this.revisados = revisados;
        this.errei = errei;
        this.dificil = dificil;
        this.bom = bom;
        this.facil = facil;
        this.tempoMs = tempoMs;
    }

    public Instant getFinalizadaEm() { return finalizadaEm; }
    public Short getRevisados() { return revisados; }
    public Short getErrei() { return errei; }
    public Short getDificil() { return dificil; }
    public Short getBom() { return bom; }
    public Short getFacil() { return facil; }
    public Integer getTempoMs() { return tempoMs; }
}
