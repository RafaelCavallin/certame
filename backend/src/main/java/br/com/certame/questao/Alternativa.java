package br.com.certame.questao;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "alternativa")
public class Alternativa {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id", nullable = false) private Questao questao;
    @Column(nullable = false) private String letra;
    @Column(nullable = false) private String texto;
    @Column(nullable = false) private short ordem;
    protected Alternativa() {}
    public Alternativa(String letra, String texto, short ordem) { this.letra = letra; this.texto = texto; this.ordem = ordem; }
    public UUID getId() { return id; }
    public Questao getQuestao() { return questao; }
    public String getLetra() { return letra; }
    public String getTexto() { return texto; }
    public short getOrdem() { return ordem; }
    public void setQuestao(Questao questao) { this.questao = questao; }
    public void setLetra(String letra) { this.letra = letra; }
    public void setTexto(String texto) { this.texto = texto; }
    public void setOrdem(short ordem) { this.ordem = ordem; }
}
