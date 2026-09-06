package br.com.certame.estudo.caderno;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "caderno_questao")
public class CadernoQuestao {
    @EmbeddedId private CadernoQuestaoId id;
    @Column(name = "adicionada_em", nullable = false, updatable = false) private Instant adicionadaEm;
    protected CadernoQuestao() {}
    public CadernoQuestao(UUID cadernoId, UUID questaoId) { id = new CadernoQuestaoId(cadernoId, questaoId); }
    @PrePersist void aoCriar() { if (adicionadaEm == null) adicionadaEm = Instant.now(); }
    public CadernoQuestaoId getId() { return id; }
    public UUID getCadernoId() { return id.getCadernoId(); }
    public UUID getQuestaoId() { return id.getQuestaoId(); }
    public Instant getAdicionadaEm() { return adicionadaEm; }
}
