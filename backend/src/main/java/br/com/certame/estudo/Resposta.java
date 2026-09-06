package br.com.certame.estudo;

import br.com.certame.questao.Questao;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resposta")
public class Resposta {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sessao_id") private SessaoEstudo sessao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id") private Questao questao;
    @Column(nullable = false) private String marcada;
    @Column(nullable = false) private boolean correta;
    @Column(name = "tempo_ms", nullable = false) private int tempoMs;
    @Column(name = "respondida_em", nullable = false, updatable = false) private Instant respondidaEm;
    protected Resposta() {}
    public Resposta(SessaoEstudo sessao, Usuario usuario, Questao questao, String marcada, boolean correta, int tempoMs) { this.sessao = sessao; this.usuario = usuario; this.questao = questao; this.marcada = marcada; this.correta = correta; this.tempoMs = tempoMs; }
    @PrePersist void aoCriar() { if (respondidaEm == null) respondidaEm = Instant.now(); }
    public UUID getId() { return id; }
    public SessaoEstudo getSessao() { return sessao; }
    public Usuario getUsuario() { return usuario; }
    public Questao getQuestao() { return questao; }
    public String getMarcada() { return marcada; }
    public boolean isCorreta() { return correta; }
    public int getTempoMs() { return tempoMs; }
    public Instant getRespondidaEm() { return respondidaEm; }
}
