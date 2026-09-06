package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.Banca;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.Orgao;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "edital")
public class Edital {
    public static final int NOME_TAMANHO_MAXIMO = 160;
    public static final int VAGAS_MAXIMO = 999999;
    public static final int URL_OFICIAL_TAMANHO_MAXIMO = 500;

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = NOME_TAMANHO_MAXIMO) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusEdital status;
    private Integer vagas;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "banca_id") private Banca banca;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "orgao_id") private Orgao orgao;
    @Column(name = "prazo_inscricao") private LocalDate prazoInscricao;
    @Column(name = "url_oficial", length = URL_OFICIAL_TAMANHO_MAXIMO) private String urlOficial;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "edital_disciplina", joinColumns = @JoinColumn(name = "edital_id"),
            inverseJoinColumns = @JoinColumn(name = "disciplina_id"))
    private Set<Disciplina> disciplinas = new HashSet<>();
    @Column(name = "excluido_em") private Instant excluidoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm;

    protected Edital() {}

    public Edital(String nome, StatusEdital status) {
        this.nome = nome;
        this.status = status;
    }

    @PrePersist void aoCriar() { var agora = Instant.now(); criadoEm = criadoEm == null ? agora : criadoEm; atualizadoEm = atualizadoEm == null ? agora : atualizadoEm; }
    @PreUpdate void aoAtualizar() { atualizadoEm = Instant.now(); }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public StatusEdital getStatus() { return status; }
    public Integer getVagas() { return vagas; }
    public Banca getBanca() { return banca; }
    public Orgao getOrgao() { return orgao; }
    public LocalDate getPrazoInscricao() { return prazoInscricao; }
    public String getUrlOficial() { return urlOficial; }
    public Set<Disciplina> getDisciplinas() { return disciplinas; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setNome(String nome) { this.nome = nome; }
    public void setStatus(StatusEdital status) { this.status = status; }
    public void setVagas(Integer vagas) { this.vagas = vagas; }
    public void setBanca(Banca banca) { this.banca = banca; }
    public void setOrgao(Orgao orgao) { this.orgao = orgao; }
    public void setPrazoInscricao(LocalDate prazoInscricao) { this.prazoInscricao = prazoInscricao; }
    public void setUrlOficial(String urlOficial) { this.urlOficial = urlOficial; }
    public void setDisciplinas(Set<Disciplina> disciplinas) { this.disciplinas = disciplinas; }
    public void setExcluidoEm(Instant excluidoEm) { this.excluidoEm = excluidoEm; }
}
