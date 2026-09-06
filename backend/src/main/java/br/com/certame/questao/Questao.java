package br.com.certame.questao;

import br.com.certame.catalogo.*;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "questao")
public class Questao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    // Gravável no insert: quem emite o código é o GeradorCodigoQuestao. Com insertable = false o
    // Hibernate omitia a coluna, o default do banco consumia um segundo nextval e a API devolvia
    // um código diferente do que ficou gravado. O default segue valendo para inserts em SQL puro.
    @Column(nullable = false, updatable = false) private String codigo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoQuestao tipo;
    @Column(nullable = false) private String enunciado;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "disciplina_id") private Disciplina disciplina;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assunto_id") private Assunto assunto;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "banca_id") private Banca banca;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "orgao_id") private Orgao orgao;
    private Short ano;
    @Enumerated(EnumType.STRING) private Dificuldade dificuldade;
    private String gabarito;
    private String comentario;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusQuestao status = StatusQuestao.RASCUNHO;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "autor_id", nullable = false) private Usuario autor;
    @Column(name = "publicado_em") private Instant publicadoEm;
    @Column(name = "excluido_em") private Instant excluidoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    @JdbcTypeCode(SqlTypes.OTHER) @Column(name = "busca", insertable = false, updatable = false, columnDefinition = "tsvector") private String busca;
    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem asc") private List<Alternativa> alternativas = new ArrayList<>();
    protected Questao() {}
    public Questao(TipoQuestao tipo, String enunciado, Usuario autor) { this.tipo = tipo; this.enunciado = enunciado; this.autor = autor; }
    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }
    public void adicionarAlternativa(Alternativa alternativa) { alternativa.setQuestao(this); alternativas.add(alternativa); }
    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoQuestao getTipo() { return tipo; }
    public String getEnunciado() { return enunciado; }
    public Disciplina getDisciplina() { return disciplina; }
    public Assunto getAssunto() { return assunto; }
    public Banca getBanca() { return banca; }
    public Orgao getOrgao() { return orgao; }
    public Short getAno() { return ano; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public String getGabarito() { return gabarito; }
    public String getComentario() { return comentario; }
    public StatusQuestao getStatus() { return status; }
    public Usuario getAutor() { return autor; }
    public Instant getPublicadoEm() { return publicadoEm; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public List<Alternativa> getAlternativas() { return alternativas; }
    public void setCodigo(String valor) { codigo = valor; }
    public void setTipo(TipoQuestao valor) { tipo = valor; }
    public void setEnunciado(String valor) { enunciado = valor; }
    public void setDisciplina(Disciplina valor) { disciplina = valor; }
    public void setAssunto(Assunto valor) { assunto = valor; }
    public void setBanca(Banca valor) { banca = valor; }
    public void setOrgao(Orgao valor) { orgao = valor; }
    public void setAno(Short valor) { ano = valor; }
    public void setDificuldade(Dificuldade valor) { dificuldade = valor; }
    public void setGabarito(String valor) { gabarito = valor; }
    public void setComentario(String valor) { comentario = valor; }
    public void setStatus(StatusQuestao valor) { status = valor; }
    public void setPublicadoEm(Instant valor) { publicadoEm = valor; }
    public void setExcluidoEm(Instant valor) { excluidoEm = valor; }
}
