package br.com.certame.importacao.lote;

import br.com.certame.questao.Fonte;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "lote_importacao")
public class LoteImportacao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String codigo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Fonte fonte;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) private String filtros;
    @Column(name = "quantidade_pedida", nullable = false) private short quantidadePedida;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusLote status = StatusLote.EXECUTANDO;
    @Column(name = "motivo_interrupcao") private String motivoInterrupcao;
    @Column(name = "somente_simulacao", nullable = false) private boolean somenteSimulacao;
    @Column(name = "total_recuperado", nullable = false) private int totalRecuperado;
    @Column(name = "total_processado", nullable = false) private int totalProcessado;
    @Column(name = "total_importado", nullable = false) private int totalImportado;
    @Column(name = "chamadas_externas", nullable = false) private int chamadasExternas;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "solicitante_id", nullable = false) private Usuario solicitante;
    @Column(name = "iniciado_em", nullable = false, updatable = false) private Instant iniciadoEm;
    @Column(name = "finalizado_em") private Instant finalizadoEm;
    protected LoteImportacao() {}
    public LoteImportacao(String codigo, Fonte fonte, String filtros, short quantidadePedida,
            boolean somenteSimulacao, Usuario solicitante) {
        this.codigo = codigo; this.fonte = fonte; this.filtros = filtros;
        this.quantidadePedida = quantidadePedida; this.somenteSimulacao = somenteSimulacao; this.solicitante = solicitante;
    }
    @PrePersist void aoCriar() { if (iniciadoEm == null) iniciadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public Fonte getFonte() { return fonte; }
    public String getFiltros() { return filtros; }
    public short getQuantidadePedida() { return quantidadePedida; }
    public StatusLote getStatus() { return status; }
    public String getMotivoInterrupcao() { return motivoInterrupcao; }
    public boolean isSomenteSimulacao() { return somenteSimulacao; }
    public int getTotalRecuperado() { return totalRecuperado; }
    public int getTotalProcessado() { return totalProcessado; }
    public int getTotalImportado() { return totalImportado; }
    public int getChamadasExternas() { return chamadasExternas; }
    public Usuario getSolicitante() { return solicitante; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getFinalizadoEm() { return finalizadoEm; }
    public void setStatus(StatusLote valor) { status = valor; }
    public void setMotivoInterrupcao(String valor) { motivoInterrupcao = valor; }
    public void setTotalRecuperado(int valor) { totalRecuperado = valor; }
    public void setTotalProcessado(int valor) { totalProcessado = valor; }
    public void setTotalImportado(int valor) { totalImportado = valor; }
    public void setChamadasExternas(int valor) { chamadasExternas = valor; }
    public void setFinalizadoEm(Instant valor) { finalizadoEm = valor; }
}
