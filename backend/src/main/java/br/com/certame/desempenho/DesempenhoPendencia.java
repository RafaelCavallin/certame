package br.com.certame.desempenho;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "desempenho_pendencia")
public class DesempenhoPendencia {
    @Id @Column(name = "usuario_id") private UUID usuarioId;
    @Column(name = "marcado_em", nullable = false) private Instant marcadoEm;
    protected DesempenhoPendencia() {}
    public DesempenhoPendencia(UUID usuarioId) { this.usuarioId = usuarioId; }
    @PrePersist void aoCriar() { if (marcadoEm == null) marcadoEm = Instant.now(); }
    public UUID getUsuarioId() { return usuarioId; } public Instant getMarcadoEm() { return marcadoEm; }
}
