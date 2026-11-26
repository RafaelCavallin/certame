package br.com.certame.importacao.lote;

import br.com.certame.importacao.ResultadoCandidata;
import br.com.certame.questao.Questao;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "item_importacao")
public class ItemImportacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lote_id", nullable = false) private LoteImportacao lote;
    @Column(nullable = false) private int ordem;
    @Column(name = "identificador_externo", nullable = false) private String identificadorExterno;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ResultadoCandidata resultado;
    @Column(nullable = false) private String motivo;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private String pendencias = "[]";
    @Column(name = "valor_externo_nao_mapeado") private String valorExternoNaoMapeado;
    @Column(name = "tipo_taxonomia_nao_mapeada") private String tipoTaxonomiaNaoMapeada;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "questao_id") private Questao questao;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    protected ItemImportacao() {}
    public ItemImportacao(LoteImportacao lote, int ordem, String identificadorExterno, ResultadoCandidata resultado, String motivo) {
        this.lote = lote; this.ordem = ordem; this.identificadorExterno = identificadorExterno;
        this.resultado = resultado; this.motivo = motivo;
    }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public LoteImportacao getLote() { return lote; }
    public int getOrdem() { return ordem; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public ResultadoCandidata getResultado() { return resultado; }
    public String getMotivo() { return motivo; }
    public String getPendencias() { return pendencias; }
    public String getValorExternoNaoMapeado() { return valorExternoNaoMapeado; }
    public String getTipoTaxonomiaNaoMapeada() { return tipoTaxonomiaNaoMapeada; }
    public Questao getQuestao() { return questao; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setPendencias(String valor) { pendencias = valor; }
    public void setValorExternoNaoMapeado(String valor) { valorExternoNaoMapeado = valor; }
    public void setTipoTaxonomiaNaoMapeada(String valor) { tipoTaxonomiaNaoMapeada = valor; }
    public void setQuestao(Questao valor) { questao = valor; }
}
