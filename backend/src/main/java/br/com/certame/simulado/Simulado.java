package br.com.certame.simulado;

import br.com.certame.catalogo.Banca;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "simulado")
public class Simulado {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, updatable = false) private String codigo;
    @Column(nullable = false) private String nome;
    private String descricao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "banca_id") private Banca banca;
    @Column(name = "numero_questoes", nullable = false) private Short numeroQuestoes;
    @Column(name = "duracao_segundos", nullable = false) private Integer duracaoSegundos;
    @Column(name = "janela_abertura") private Instant janelaAbertura;
    @Column(name = "janela_encerramento") private Instant janelaEncerramento;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusSimulado status = StatusSimulado.RASCUNHO;
    @Column(name = "publicado_em") private Instant publicadoEm;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criado_por") private Usuario criadoPor;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    @Column(name = "excluido_em") private Instant excluidoEm;
    protected Simulado() {}
    public Simulado(String codigo, String nome, Short numeroQuestoes, Integer duracaoSegundos, Usuario criadoPor) { this.codigo = codigo; this.nome = nome; this.numeroQuestoes = numeroQuestoes; this.duracaoSegundos = duracaoSegundos; this.criadoPor = criadoPor; }
    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }
    public UUID getId() { return id; } public String getCodigo() { return codigo; } public String getNome() { return nome; } public String getDescricao() { return descricao; }
    public Banca getBanca() { return banca; } public Short getNumeroQuestoes() { return numeroQuestoes; } public Integer getDuracaoSegundos() { return duracaoSegundos; }
    public Instant getJanelaAbertura() { return janelaAbertura; } public Instant getJanelaEncerramento() { return janelaEncerramento; } public StatusSimulado getStatus() { return status; }
    public Instant getPublicadoEm() { return publicadoEm; } public Usuario getCriadoPor() { return criadoPor; } public Instant getCriadoEm() { return criadoEm; } public Instant getAtualizadoEm() { return atualizadoEm; } public Instant getExcluidoEm() { return excluidoEm; }
    public void setNome(String valor) { nome = valor; }
    public void setNumeroQuestoes(Short valor) { numeroQuestoes = valor; }
    public void setDuracaoSegundos(Integer valor) { duracaoSegundos = valor; }
    public void setDescricao(String valor) { descricao = valor; }
    public void setBanca(Banca valor) { banca = valor; }
    public void setJanelaAbertura(Instant valor) { janelaAbertura = valor; }
    public void setJanelaEncerramento(Instant valor) { janelaEncerramento = valor; } public void setStatus(StatusSimulado valor) { status = valor; } public void setPublicadoEm(Instant valor) { publicadoEm = valor; }
    public void setExcluidoEm(Instant valor) { excluidoEm = valor; }
}
