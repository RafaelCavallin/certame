package br.com.certame.catalogo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "disciplina")
public class Disciplina {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String nome;
    @Column(name = "arquivado_em") private Instant arquivadoEm;
    @Column(name = "excluido_em") private Instant excluidoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected Disciplina() {}
    public Disciplina(String nome) { this.nome = nome; }
    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Instant getArquivadoEm() { return arquivadoEm; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public void setNome(String nome) { this.nome = nome; }
    public void setArquivadoEm(Instant valor) { arquivadoEm = valor; }
    public void setExcluidoEm(Instant valor) { excluidoEm = valor; }
}
