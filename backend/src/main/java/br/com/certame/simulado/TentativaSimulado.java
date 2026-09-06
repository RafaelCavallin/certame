package br.com.certame.simulado;

import br.com.certame.estudo.SessaoEstudo;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "tentativa_simulado")
public class TentativaSimulado {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "simulado_id") private Simulado simulado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sessao_id", nullable = false) private SessaoEstudo sessao;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoTentativa tipo = TipoTentativa.VALIDA;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusTentativa status = StatusTentativa.EM_ANDAMENTO;
    private Short nota; private Short acertos; private Short erros; @Column(name = "nao_respondidas") private Short naoRespondidas;
    @Column(name = "tempo_total_segundos") private Integer tempoTotalSegundos;
    @Column(name = "iniciada_em", nullable = false, updatable = false) private Instant iniciadaEm;
    @Column(name = "finalizada_em") private Instant finalizadaEm;
    protected TentativaSimulado() {}
    public TentativaSimulado(Simulado simulado, Usuario usuario, SessaoEstudo sessao, TipoTentativa tipo, Instant iniciadaEm) { this.simulado = simulado; this.usuario = usuario; this.sessao = sessao; this.tipo = tipo; this.iniciadaEm = iniciadaEm; }
    public UUID getId() { return id; } public Simulado getSimulado() { return simulado; } public Usuario getUsuario() { return usuario; } public SessaoEstudo getSessao() { return sessao; }
    public TipoTentativa getTipo() { return tipo; } public StatusTentativa getStatus() { return status; } public Short getNota() { return nota; } public Short getAcertos() { return acertos; } public Short getErros() { return erros; }
    public Short getNaoRespondidas() { return naoRespondidas; } public Integer getTempoTotalSegundos() { return tempoTotalSegundos; } public Instant getIniciadaEm() { return iniciadaEm; } public Instant getFinalizadaEm() { return finalizadaEm; }
    public void finalizar(Short nota, Short acertos, Short erros, Short naoRespondidas, Integer tempoTotalSegundos, Instant finalizadaEm) { this.nota = nota; this.acertos = acertos; this.erros = erros; this.naoRespondidas = naoRespondidas; this.tempoTotalSegundos = tempoTotalSegundos; this.status = StatusTentativa.FINALIZADA; this.finalizadaEm = finalizadaEm; }
    public void setStatus(StatusTentativa valor) { status = valor; }
}
