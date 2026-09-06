package br.com.certame.flashcard;

import br.com.certame.flashcard.deck.Deck;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flashcard")
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @Column(nullable = false)
    private String frente;
    @Column(nullable = false)
    private String verso;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", nullable = false)
    private String[] tags = new String[0];
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFlashcard status = StatusFlashcard.ATIVO;

    @Embedded
    private OrigemFlashcard origem = new OrigemFlashcard();
    @Embedded
    private AgendamentoFsrs agendamento = new AgendamentoFsrs();
    @Embedded
    private RegistroTemporalFlashcard registroTemporal = new RegistroTemporalFlashcard();

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "busca", insertable = false, updatable = false, columnDefinition = "tsvector")
    private String busca;
    protected Flashcard() {
    }
    public Flashcard(Usuario usuario, String frente, String verso) {
        this.usuario = usuario;
        this.frente = frente;
        this.verso = verso;
    }

    @PrePersist
    void aoCriar() {
        registroTemporal.aoCriar(Instant.now());
        agendamento.agendarPara(agendamento.getProximaRevisaoEm());
    }

    @PreUpdate
    void aoAtualizar() {
        registroTemporal.aoAtualizar(Instant.now());
    }

    public void agendarPara(Instant proximaRevisaoEm) {
        agendamento.agendarPara(proximaRevisaoEm);
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Deck getDeck() { return deck; }
    public String getFrente() { return frente; }
    public String getVerso() { return verso; }
    public String[] getTags() { return tags; }
    public StatusFlashcard getStatus() { return status; }
    /** Hibernate devolve null quando as 4 colunas de origem estão nulas (card avulso); nunca expor null aqui. */
    public OrigemFlashcard getOrigem() { return origem == null ? origem = new OrigemFlashcard() : origem; }
    public AgendamentoFsrs getAgendamento() { return agendamento; }
    public RegistroTemporalFlashcard getRegistroTemporal() { return registroTemporal; }

    public void setDeck(Deck deck) { this.deck = deck; }
    public void setFrente(String frente) { this.frente = frente; }
    public void setVerso(String verso) { this.verso = verso; }
    public void setTags(String[] tags) { this.tags = tags; }
    public void setStatus(StatusFlashcard status) { this.status = status; }
}
