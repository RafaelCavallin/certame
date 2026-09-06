package br.com.certame.desempenho;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "desempenho_semanal")
public class DesempenhoSemanal {
    @EmbeddedId private Id id;
    private int volume;
    private int acertos;
    @Column(name = "pct_acerto") private Short pctAcerto;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected DesempenhoSemanal() {}
    public DesempenhoSemanal(UUID usuarioId, LocalDate semana, int volume, int acertos, Short pctAcerto) { id = new Id(usuarioId, semana); this.volume = volume; this.acertos = acertos; this.pctAcerto = pctAcerto; }
    @PrePersist @PreUpdate void atualizarData() { atualizadoEm = Instant.now(); }
    public Id getId() { return id; } public UUID getUsuarioId() { return id.usuarioId(); } public LocalDate getSemana() { return id.semana(); }
    public int getVolume() { return volume; } public int getAcertos() { return acertos; } public Short getPctAcerto() { return pctAcerto; } public Instant getAtualizadoEm() { return atualizadoEm; }
    public record Id(UUID usuarioId, LocalDate semana) implements java.io.Serializable {}
}
