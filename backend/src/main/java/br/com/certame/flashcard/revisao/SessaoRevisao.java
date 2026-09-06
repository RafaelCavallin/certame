package br.com.certame.flashcard.revisao;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.flashcard.deck.Deck;
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

@Entity
@Table(name = "flashcard_sessao")
public class SessaoRevisao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String escopo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @Column(name = "iniciada_em", nullable = false, updatable = false)
    private Instant iniciadaEm;

    @Embedded
    private ResultadoSessaoRevisao resultado = new ResultadoSessaoRevisao();

    protected SessaoRevisao() {
    }

    public SessaoRevisao(Usuario usuario, String escopo, Deck deck, Disciplina disciplina) {
        this.usuario = usuario;
        this.escopo = escopo;
        this.deck = deck;
        this.disciplina = disciplina;
    }

    @PrePersist
    void aoCriar() {
        if (iniciadaEm == null) {
            iniciadaEm = Instant.now();
        }
    }

    public void finalizar(Instant finalizadaEm, short revisados, short errei, short dificil, short bom, short facil, int tempoMs) {
        resultado.finalizar(finalizadaEm, revisados, errei, dificil, bom, facil, tempoMs);
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getEscopo() { return escopo; }
    public Deck getDeck() { return deck; }
    public Disciplina getDisciplina() { return disciplina; }
    public Instant getIniciadaEm() { return iniciadaEm; }
    public Instant getFinalizadaEm() { return resultado.getFinalizadaEm(); }
    public Short getRevisados() { return resultado.getRevisados(); }
    public Short getErrei() { return resultado.getErrei(); }
    public Short getDificil() { return resultado.getDificil(); }
    public Short getBom() { return resultado.getBom(); }
    public Short getFacil() { return resultado.getFacil(); }
    public Integer getTempoMs() { return resultado.getTempoMs(); }
}
