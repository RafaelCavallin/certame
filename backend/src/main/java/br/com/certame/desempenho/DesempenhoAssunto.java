package br.com.certame.desempenho;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "desempenho_assunto")
public class DesempenhoAssunto {
    @EmbeddedId private Id id; @Column(name = "disciplina_id", nullable = false) private UUID disciplinaId;
    private int volume; private int acertos; @Column(name = "pct_acerto") private Short pctAcerto;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected DesempenhoAssunto() {}
    public DesempenhoAssunto(UUID usuarioId, UUID assuntoId, UUID disciplinaId, int volume, int acertos, Short pctAcerto) { id = new Id(usuarioId, assuntoId); this.disciplinaId = disciplinaId; this.volume = volume; this.acertos = acertos; this.pctAcerto = pctAcerto; }
    @PrePersist @PreUpdate void atualizarData() { atualizadoEm = Instant.now(); }
    public Id getId() { return id; } public UUID getUsuarioId() { return id.usuarioId(); } public UUID getAssuntoId() { return id.assuntoId(); } public UUID getDisciplinaId() { return disciplinaId; }
    public int getVolume() { return volume; } public int getAcertos() { return acertos; } public Short getPctAcerto() { return pctAcerto; } public Instant getAtualizadoEm() { return atualizadoEm; }
    public record Id(UUID usuarioId, UUID assuntoId) implements java.io.Serializable {}
}
