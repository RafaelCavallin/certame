package br.com.certame.estudo.caderno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caderno")
public class Caderno {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "usuario_id", nullable = false, updatable = false) private UUID usuarioId;
    @Column(nullable = false) private String nome;
    private String descricao;
    @Column(name = "excluido_em") private Instant excluidoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;

    protected Caderno() {}
    public Caderno(UUID usuarioId, String nome, String descricao) { this.usuarioId = usuarioId; this.nome = nome; this.descricao = descricao; }
    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setExcluidoEm(Instant excluidoEm) { this.excluidoEm = excluidoEm; }
    public void tocar(Instant instante) { atualizadoEm = instante; }
}
