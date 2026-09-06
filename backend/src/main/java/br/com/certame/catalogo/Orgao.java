package br.com.certame.catalogo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "orgao")
public class Orgao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String nome;
    private String sigla;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    protected Orgao() {}
    public Orgao(String nome, String sigla) { this.nome = nome; this.sigla = sigla; }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getSigla() { return sigla; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setNome(String nome) { this.nome = nome; }
    public void setSigla(String sigla) { this.sigla = sigla; }
}
