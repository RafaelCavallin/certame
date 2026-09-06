package br.com.certame.estudo;

import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao_estudo")
public class SessaoEstudo {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ModoSessao modo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrigemSessao origem;
    @Column(name = "origem_ref") private String origemRef;
    @Column(nullable = false) private Short tamanho;
    @Column(name = "pausado_ms", nullable = false) private int pausadoMs;
    @Column(name = "pausada_em") private Instant pausadaEm;
    @Column(name = "iniciada_em", nullable = false, updatable = false) private Instant iniciadaEm;
    @Column(name = "ultima_atividade_em", nullable = false) private Instant ultimaAtividadeEm;
    @Column(name = "finalizada_em") private Instant finalizadaEm;
    @Column(name = "expirada_em") private Instant expiradaEm;
    protected SessaoEstudo() {}
    public SessaoEstudo(Usuario usuario, ModoSessao modo, OrigemSessao origem, String origemRef, Short tamanho) { this.usuario = usuario; this.modo = modo; this.origem = origem; this.origemRef = origemRef; this.tamanho = tamanho; }
    @PrePersist void aoCriar() { var agora = Instant.now(); iniciadaEm = iniciadaEm == null ? agora : iniciadaEm; ultimaAtividadeEm = ultimaAtividadeEm == null ? agora : ultimaAtividadeEm; }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public ModoSessao getModo() { return modo; }
    public OrigemSessao getOrigem() { return origem; }
    public String getOrigemRef() { return origemRef; }
    public Short getTamanho() { return tamanho; }
    public int getPausadoMs() { return pausadoMs; }
    public Instant getPausadaEm() { return pausadaEm; }
    public Instant getIniciadaEm() { return iniciadaEm; }
    public Instant getUltimaAtividadeEm() { return ultimaAtividadeEm; }
    public Instant getFinalizadaEm() { return finalizadaEm; }
    public Instant getExpiradaEm() { return expiradaEm; }
    public void setModo(ModoSessao modo) { this.modo = modo; }
    public void setPausadoMs(int pausadoMs) { this.pausadoMs = pausadoMs; }
    public void setPausadaEm(Instant valor) { pausadaEm = valor; }
    public void setUltimaAtividadeEm(Instant valor) { ultimaAtividadeEm = valor; }
    public void setFinalizadaEm(Instant valor) { finalizadaEm = valor; }
    public void setExpiradaEm(Instant valor) { expiradaEm = valor; }
}
