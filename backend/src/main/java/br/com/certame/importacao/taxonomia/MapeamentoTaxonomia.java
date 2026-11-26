package br.com.certame.importacao.taxonomia;

import br.com.certame.catalogo.Assunto;
import br.com.certame.catalogo.Banca;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.Orgao;
import br.com.certame.questao.Fonte;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "mapeamento_taxonomia")
public class MapeamentoTaxonomia {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Fonte fonte;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoTaxonomia tipo;
    @Column(name = "valor_externo", nullable = false) private String valorExterno;
    @Column(name = "valor_externo_original", nullable = false) private String valorExternoOriginal;
    @Column(name = "valor_externo_pai") private String valorExternoPai;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "disciplina_id") private Disciplina disciplina;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assunto_id") private Assunto assunto;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "banca_id") private Banca banca;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "orgao_id") private Orgao orgao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "criado_por", nullable = false) private Usuario criadoPor;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;
    protected MapeamentoTaxonomia() {}
    public MapeamentoTaxonomia(Fonte fonte, TipoTaxonomia tipo, String valorExterno, String valorExternoOriginal,
            String valorExternoPai, Usuario criadoPor) {
        this.fonte = fonte; this.tipo = tipo; this.valorExterno = valorExterno;
        this.valorExternoOriginal = valorExternoOriginal; this.valorExternoPai = valorExternoPai; this.criadoPor = criadoPor;
    }
    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Fonte getFonte() { return fonte; }
    public TipoTaxonomia getTipo() { return tipo; }
    public String getValorExterno() { return valorExterno; }
    public String getValorExternoOriginal() { return valorExternoOriginal; }
    public String getValorExternoPai() { return valorExternoPai; }
    public Disciplina getDisciplina() { return disciplina; }
    public Assunto getAssunto() { return assunto; }
    public Banca getBanca() { return banca; }
    public Orgao getOrgao() { return orgao; }
    public Usuario getCriadoPor() { return criadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setValorExterno(String valor) { valorExterno = valor; }
    public void setDisciplina(Disciplina valor) { disciplina = valor; }
    public void setAssunto(Assunto valor) { assunto = valor; }
    public void setBanca(Banca valor) { banca = valor; }
    public void setOrgao(Orgao valor) { orgao = valor; }
}
