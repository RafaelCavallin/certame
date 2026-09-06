package br.com.certame.estudo.caderno;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class CadernoQuestaoId implements Serializable {
    private UUID cadernoId;
    private UUID questaoId;
    protected CadernoQuestaoId() {}
    public CadernoQuestaoId(UUID cadernoId, UUID questaoId) { this.cadernoId = cadernoId; this.questaoId = questaoId; }
    public UUID getCadernoId() { return cadernoId; }
    public UUID getQuestaoId() { return questaoId; }
    @Override public boolean equals(Object outro) { if (this == outro) return true; if (!(outro instanceof CadernoQuestaoId id)) return false; return cadernoId.equals(id.cadernoId) && questaoId.equals(id.questaoId); }
    @Override public int hashCode() { return 31 * cadernoId.hashCode() + questaoId.hashCode(); }
}
