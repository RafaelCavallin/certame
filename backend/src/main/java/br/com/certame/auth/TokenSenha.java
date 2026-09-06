package br.com.certame.auth;

import br.com.certame.usuario.Usuario;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_senha")
public class TokenSenha {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinalidadeTokenSenha finalidade;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "usado_em") private Instant usadoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;

    protected TokenSenha() { }
    public TokenSenha(Usuario usuario, String tokenHash, FinalidadeTokenSenha finalidade, Instant expiraEm) {
        this.usuario = usuario; this.tokenHash = tokenHash; this.finalidade = finalidade; this.expiraEm = expiraEm;
    }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public FinalidadeTokenSenha getFinalidade() { return finalidade; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getUsadoEm() { return usadoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setUsadoEm(Instant usadoEm) { this.usadoEm = usadoEm; }
}
