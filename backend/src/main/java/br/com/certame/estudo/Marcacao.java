package br.com.certame.estudo;

import br.com.certame.questao.Questao;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marcacao", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "questao_id", "tipo"}))
public class Marcacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id") private Questao questao;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoMarcacao tipo;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    protected Marcacao() {}
    public Marcacao(Usuario usuario, Questao questao, TipoMarcacao tipo) { this.usuario = usuario; this.questao = questao; this.tipo = tipo; }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Questao getQuestao() { return questao; }
    public TipoMarcacao getTipo() { return tipo; }
    public Instant getCriadoEm() { return criadoEm; }
}
