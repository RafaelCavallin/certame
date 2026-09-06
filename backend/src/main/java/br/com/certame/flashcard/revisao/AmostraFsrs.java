package br.com.certame.flashcard.revisao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Amostra imutável do estado FSRS (antes/depois) capturada por uma revisão (RN-08). */
@Embeddable
public class AmostraFsrs {

    @Column(name = "dias_decorridos", nullable = false)
    private Short diasDecorridos;

    @Column(name = "estabilidade_ant", nullable = false)
    private Float estabilidadeAnt;

    @Column(name = "estabilidade_nova", nullable = false)
    private Float estabilidadeNova;

    @Column(name = "dificuldade_ant", nullable = false)
    private Float dificuldadeAnt;

    @Column(name = "dificuldade_nova", nullable = false)
    private Float dificuldadeNova;

    @Column(name = "intervalo_novo", nullable = false)
    private Short intervaloNovo;

    @Column(name = "intervalo_minutos", nullable = false)
    private Integer intervaloMinutos;

    @Column(name = "tempo_ms", nullable = false)
    private Integer tempoMs;

    @Column(name = "segundos_decorridos", nullable = false)
    private Integer segundosDecorridos;

    protected AmostraFsrs() {
    }

    public AmostraFsrs(short diasDecorridos, float estabilidadeAnt, float estabilidadeNova, float dificuldadeAnt,
            float dificuldadeNova, short intervaloNovo, int intervaloMinutos, int tempoMs, int segundosDecorridos) {
        this.diasDecorridos = diasDecorridos;
        this.estabilidadeAnt = estabilidadeAnt;
        this.estabilidadeNova = estabilidadeNova;
        this.dificuldadeAnt = dificuldadeAnt;
        this.dificuldadeNova = dificuldadeNova;
        this.intervaloNovo = intervaloNovo;
        this.intervaloMinutos = intervaloMinutos;
        this.tempoMs = tempoMs;
        this.segundosDecorridos = segundosDecorridos;
    }

    public Short getDiasDecorridos() { return diasDecorridos; }
    public Float getEstabilidadeAnt() { return estabilidadeAnt; }
    public Float getEstabilidadeNova() { return estabilidadeNova; }
    public Float getDificuldadeAnt() { return dificuldadeAnt; }
    public Float getDificuldadeNova() { return dificuldadeNova; }
    public Short getIntervaloNovo() { return intervaloNovo; }
    public Integer getIntervaloMinutos() { return intervaloMinutos; }
    public Integer getTempoMs() { return tempoMs; }
    public Integer getSegundosDecorridos() { return segundosDecorridos; }
}
