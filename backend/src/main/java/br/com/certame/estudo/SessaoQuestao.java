package br.com.certame.estudo;

import br.com.certame.questao.Questao;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "sessao_questao")
public class SessaoQuestao {
    @EmbeddedId private SessaoQuestaoId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("sessaoId") @JoinColumn(name = "sessao_id") private SessaoEstudo sessao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id") private Questao questao;
    protected SessaoQuestao() {}
    public SessaoQuestao(SessaoEstudo sessao, Short ordem, Questao questao) { this.sessao = sessao; this.questao = questao; this.id = new SessaoQuestaoId(sessao.getId(), ordem); }
    public SessaoQuestaoId getId() { return id; }
    public SessaoEstudo getSessao() { return sessao; }
    public Questao getQuestao() { return questao; }
    public UUID getQuestaoId() { return questao.getId(); }
}
