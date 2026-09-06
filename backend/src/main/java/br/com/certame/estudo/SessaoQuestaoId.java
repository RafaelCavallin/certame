package br.com.certame.estudo;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class SessaoQuestaoId implements Serializable {
    private UUID sessaoId;
    private Short ordem;
    protected SessaoQuestaoId() {}
    public SessaoQuestaoId(UUID sessaoId, Short ordem) { this.sessaoId = sessaoId; this.ordem = ordem; }
    public UUID getSessaoId() { return sessaoId; }
    public Short getOrdem() { return ordem; }
    @Override public boolean equals(Object objeto) { if (this == objeto) return true; if (!(objeto instanceof SessaoQuestaoId outro)) return false; return sessaoId.equals(outro.sessaoId) && ordem.equals(outro.ordem); }
    @Override public int hashCode() { return 31 * sessaoId.hashCode() + ordem.hashCode(); }
}
