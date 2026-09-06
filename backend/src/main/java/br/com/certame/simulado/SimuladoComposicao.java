package br.com.certame.simulado;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.questao.Dificuldade;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "simulado_composicao")
public class SimuladoComposicao {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "simulado_id") private Simulado simulado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "disciplina_id") private Disciplina disciplina;
    @Enumerated(EnumType.STRING) @Column(name = "dificuldade") private Dificuldade dificuldade;
    @Column(nullable = false) private Short quantidade;
    protected SimuladoComposicao() {}
    public SimuladoComposicao(Simulado simulado, Disciplina disciplina, Dificuldade dificuldade, Short quantidade) {
        this.simulado = simulado;
        this.disciplina = disciplina;
        this.dificuldade = dificuldade;
        this.quantidade = quantidade;
    }
    public UUID getId() { return id; }
    public Simulado getSimulado() { return simulado; }
    public Disciplina getDisciplina() { return disciplina; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public Short getQuantidade() { return quantidade; }
    public void setQuantidade(Short valor) { quantidade = valor; }
}
