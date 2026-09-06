package br.com.certame.desempenho;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "desempenho_disciplina")
public class DesempenhoDisciplina {
    @EmbeddedId private Id id; private int volume; private int acertos;
    @Column(name = "pct_acerto") private Short pctAcerto; @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected DesempenhoDisciplina() {}
    public DesempenhoDisciplina(UUID usuarioId, UUID disciplinaId, int volume, int acertos, Short pctAcerto) { id = new Id(usuarioId, disciplinaId); this.volume = volume; this.acertos = acertos; this.pctAcerto = pctAcerto; }
    @PrePersist @PreUpdate void atualizarData() { atualizadoEm = Instant.now(); }
    public Id getId() { return id; } public UUID getUsuarioId() { return id.usuarioId(); } public UUID getDisciplinaId() { return id.disciplinaId(); }
    public int getVolume() { return volume; } public int getAcertos() { return acertos; } public Short getPctAcerto() { return pctAcerto; } public Instant getAtualizadoEm() { return atualizadoEm; }
    public record Id(UUID usuarioId, UUID disciplinaId) implements java.io.Serializable {}
}
