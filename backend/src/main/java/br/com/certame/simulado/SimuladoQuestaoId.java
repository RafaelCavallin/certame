package br.com.certame.simulado;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class SimuladoQuestaoId implements Serializable {
    private UUID simuladoId;
    private Short ordem;
    protected SimuladoQuestaoId() {}
    public SimuladoQuestaoId(UUID simuladoId, Short ordem) { this.simuladoId = simuladoId; this.ordem = ordem; }
    public UUID getSimuladoId() { return simuladoId; }
    public Short getOrdem() { return ordem; }
    @Override public boolean equals(Object objeto) { if (this == objeto) return true; if (!(objeto instanceof SimuladoQuestaoId outro)) return false; return simuladoId.equals(outro.simuladoId) && ordem.equals(outro.ordem); }
    @Override public int hashCode() { return 31 * simuladoId.hashCode() + ordem.hashCode(); }
}
