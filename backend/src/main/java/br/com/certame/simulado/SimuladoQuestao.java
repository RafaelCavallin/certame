package br.com.certame.simulado;

import br.com.certame.questao.Questao;
import jakarta.persistence.*;

@Entity @Table(name = "simulado_questao")
public class SimuladoQuestao {
    @EmbeddedId private SimuladoQuestaoId id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @MapsId("simuladoId") @JoinColumn(name = "simulado_id") private Simulado simulado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id") private Questao questao;
    protected SimuladoQuestao() {}
    public SimuladoQuestao(Simulado simulado, Short ordem, Questao questao) { this.simulado = simulado; this.questao = questao; this.id = new SimuladoQuestaoId(simulado.getId(), ordem); }
    public SimuladoQuestaoId getId() { return id; } public Simulado getSimulado() { return simulado; } public Questao getQuestao() { return questao; }
}
