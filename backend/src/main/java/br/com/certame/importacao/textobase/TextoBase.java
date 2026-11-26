package br.com.certame.importacao.textobase;

import br.com.certame.questao.Fonte;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "texto_base")
public class TextoBase {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Fonte fonte;
    @Column(name = "identificador_externo") private String identificadorExterno;
    private String titulo;
    @Column(nullable = false) private String conteudo;
    private String referencia;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    protected TextoBase() {}
    public TextoBase(Fonte fonte, String identificadorExterno, String titulo, String conteudo, String referencia) {
        this.fonte = fonte; this.identificadorExterno = identificadorExterno; this.titulo = titulo;
        this.conteudo = conteudo; this.referencia = referencia;
    }
    @PrePersist void aoCriar() { if (criadoEm == null) criadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public Fonte getFonte() { return fonte; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getTitulo() { return titulo; }
    public String getConteudo() { return conteudo; }
    public String getReferencia() { return referencia; }
    public Instant getCriadoEm() { return criadoEm; }
}
