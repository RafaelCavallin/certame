package br.com.certame.importacao.divergencia;

import br.com.certame.importacao.lote.LoteImportacao;
import br.com.certame.questao.Questao;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "divergencia_externa")
public class DivergenciaExterna {
    public enum Situacao { PENDENTE, APLICADA, IGNORADA }
    public enum Prioridade { NORMAL, ALTA }

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "questao_id", nullable = false) private Questao questao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lote_id") private LoteImportacao lote;
    @Column(name = "hash_anterior", nullable = false) private String hashAnterior;
    @Column(name = "hash_novo", nullable = false) private String hashNovo;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "conteudo_externo", nullable = false) private String conteudoExterno;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Situacao situacao = Situacao.PENDENTE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Prioridade prioridade = Prioridade.NORMAL;
    @Column(name = "detectada_em", nullable = false, updatable = false) private Instant detectadaEm;
    @Column(name = "decidida_em") private Instant decididaEm;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decidido_por") private Usuario decididoPor;
    protected DivergenciaExterna() {}
    public DivergenciaExterna(Questao questao, LoteImportacao lote, String hashAnterior, String hashNovo, String conteudoExterno) {
        this.questao = questao; this.lote = lote; this.hashAnterior = hashAnterior;
        this.hashNovo = hashNovo; this.conteudoExterno = conteudoExterno;
    }
    @PrePersist void aoCriar() { if (detectadaEm == null) detectadaEm = Instant.now(); }
    public UUID getId() { return id; }
    public Questao getQuestao() { return questao; }
    public LoteImportacao getLote() { return lote; }
    public String getHashAnterior() { return hashAnterior; }
    public String getHashNovo() { return hashNovo; }
    public String getConteudoExterno() { return conteudoExterno; }
    public Situacao getSituacao() { return situacao; }
    public Prioridade getPrioridade() { return prioridade; }
    public Instant getDetectadaEm() { return detectadaEm; }
    public Instant getDecididaEm() { return decididaEm; }
    public Usuario getDecididoPor() { return decididoPor; }
    public void setSituacao(Situacao valor) { situacao = valor; }
    public void setPrioridade(Prioridade valor) { prioridade = valor; }
    public void setDecididaEm(Instant valor) { decididaEm = valor; }
    public void setDecididoPor(Usuario valor) { decididoPor = valor; }
}
