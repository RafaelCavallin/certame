package br.com.certame.comum.auditoria;

import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "auditoria")
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ator_id") private Usuario ator;
    @Column(nullable = false) private String acao;
    @Column(nullable = false) private String entidade;
    @Column(name = "entidade_id") private UUID entidadeId;
    @JdbcTypeCode(SqlTypes.JSON) private String payload;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    protected Auditoria() {}
    public Auditoria(Usuario ator, String acao, String entidade, UUID entidadeId, String payload) { this.ator = ator; this.acao = acao; this.entidade = entidade; this.entidadeId = entidadeId; this.payload = payload; }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Usuario getAtor() { return ator; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public UUID getEntidadeId() { return entidadeId; }
    public String getPayload() { return payload; }
    public Instant getCriadoEm() { return criadoEm; }
}
