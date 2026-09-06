package br.com.certame.auth;

import br.com.certame.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "usado_em") private Instant usadoEm;
    @Column(name = "revogado_em") private Instant revogadoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    private InetAddress ip;
    @Column(name = "user_agent") private String userAgent;

    protected RefreshToken() { }
    public RefreshToken(Usuario usuario, String tokenHash, Instant expiraEm, InetAddress ip, String userAgent) {
        this.usuario = usuario; this.tokenHash = tokenHash; this.expiraEm = expiraEm; this.ip = ip; this.userAgent = userAgent;
    }
    @jakarta.persistence.PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getUsadoEm() { return usadoEm; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public InetAddress getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public void setUsadoEm(Instant usadoEm) { this.usadoEm = usadoEm; }
    public void setRevogadoEm(Instant revogadoEm) { this.revogadoEm = revogadoEm; }
}
