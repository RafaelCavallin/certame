package br.com.certame.flashcard.deck;

import br.com.certame.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deck")
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String nome;

    @Column
    private String descricao;

    @Column(name = "novos_dia", nullable = false)
    private Short novosDia = 10;

    @Column(name = "revisoes_dia", nullable = false)
    private Short revisoesDia = 60;

    @Column(name = "intervalo_max_dias", nullable = false)
    private Short intervaloMaxDias = 180;

    @Column(name = "retencao_alvo", nullable = false)
    private BigDecimal retencaoAlvo = new BigDecimal("0.90");

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Deck() {
    }

    public Deck(Usuario usuario, String nome) {
        this.usuario = usuario;
        this.nome = nome;
    }

    public Deck(Usuario usuario, String nome, String descricao) {
        this.usuario = usuario;
        this.nome = nome;
        this.descricao = descricao;
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Short getNovosDia() { return novosDia; }
    public Short getRevisoesDia() { return revisoesDia; }
    public Short getIntervaloMaxDias() { return intervaloMaxDias; }
    public BigDecimal getRetencaoAlvo() { return retencaoAlvo; }
    public Instant getCriadoEm() { return criadoEm; }

    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setNovosDia(Short novosDia) { this.novosDia = novosDia; }
    public void setRevisoesDia(Short revisoesDia) { this.revisoesDia = revisoesDia; }
    public void setIntervaloMaxDias(Short intervaloMaxDias) { this.intervaloMaxDias = intervaloMaxDias; }
    public void setRetencaoAlvo(BigDecimal retencaoAlvo) { this.retencaoAlvo = retencaoAlvo; }

    public void aplicarLimites(LimitesDeck limites) {
        var normalizado = limites.normalizar();
        this.novosDia = normalizado.novosDia();
        this.revisoesDia = normalizado.revisoesDia();
        this.intervaloMaxDias = normalizado.intervaloMaxDias();
        this.retencaoAlvo = normalizado.retencaoAlvo();
    }
}
