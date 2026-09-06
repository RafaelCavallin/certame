package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.Flashcard;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Log imutável de revisões (RN-08 e restrição técnica do PRD): nenhum campo é alterado após o insert. */
@Entity
@Table(name = "flashcard_revisao")
public class FlashcardRevisao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id", nullable = false)
    private Flashcard flashcard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id")
    private SessaoRevisao sessao;

    @Column(nullable = false)
    private String nota;

    @Embedded
    private AmostraFsrs amostra;

    @Column(name = "revisado_em", nullable = false, updatable = false)
    private Instant revisadoEm;

    protected FlashcardRevisao() {
    }

    public FlashcardRevisao(Flashcard flashcard, Usuario usuario, SessaoRevisao sessao, String nota,
            short diasDecorridos, float estabilidadeAnt, float estabilidadeNova,
            float dificuldadeAnt, float dificuldadeNova, short intervaloNovo,
            int intervaloMinutos, int tempoMs, int segundosDecorridos) {
        this.flashcard = flashcard;
        this.usuario = usuario;
        this.sessao = sessao;
        this.nota = nota;
        this.amostra = new AmostraFsrs(diasDecorridos, estabilidadeAnt, estabilidadeNova, dificuldadeAnt,
                dificuldadeNova, intervaloNovo, intervaloMinutos, tempoMs, segundosDecorridos);
    }

    @PrePersist
    void aoCriar() {
        if (revisadoEm == null) {
            revisadoEm = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public Flashcard getFlashcard() { return flashcard; }
    public Usuario getUsuario() { return usuario; }
    public SessaoRevisao getSessao() { return sessao; }
    public String getNota() { return nota; }
    public Instant getRevisadoEm() { return revisadoEm; }
    public Short getDiasDecorridos() { return amostra.getDiasDecorridos(); }
    public Float getEstabilidadeAnt() { return amostra.getEstabilidadeAnt(); }
    public Float getEstabilidadeNova() { return amostra.getEstabilidadeNova(); }
    public Float getDificuldadeAnt() { return amostra.getDificuldadeAnt(); }
    public Float getDificuldadeNova() { return amostra.getDificuldadeNova(); }
    public Short getIntervaloNovo() { return amostra.getIntervaloNovo(); }
    public Integer getIntervaloMinutos() { return amostra.getIntervaloMinutos(); }
    public Integer getTempoMs() { return amostra.getTempoMs(); }
    public Integer getSegundosDecorridos() { return amostra.getSegundosDecorridos(); }
}
