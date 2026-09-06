package br.com.certame.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuario perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    @Column(name = "anonimizado_em")
    private Instant anonimizadoEm;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, PerfilUsuario perfil, StatusUsuario status) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.status = status;
    }

    @PrePersist
    void aoCriar() {
        var agora = Instant.now();
        if (criadoEm == null) criadoEm = agora;
        if (atualizadoEm == null) atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public StatusUsuario getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public Instant getAnonimizadoEm() { return anonimizadoEm; }

    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setStatus(StatusUsuario status) { this.status = status; }
    public void setExcluidoEm(Instant excluidoEm) { this.excluidoEm = excluidoEm; }

    public void editar(String nome, String email, PerfilUsuario perfil, StatusUsuario status) {
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.status = status;
    }

    public void excluir() {
        this.excluidoEm = Instant.now();
    }

    public void anonimizar(String nome, String email, Instant quando) {
        this.nome = nome;
        this.email = email;
        this.anonimizadoEm = quando;
    }
}
