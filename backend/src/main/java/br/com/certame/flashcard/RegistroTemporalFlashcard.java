package br.com.certame.flashcard;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Marcas de tempo de ciclo de vida do flashcard: criação, atualização, arquivamento e exclusão lógica. */
@Embeddable
public class RegistroTemporalFlashcard {

    @Column(name = "arquivado_em")
    private Instant arquivadoEm;

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    void aoCriar(Instant agora) {
        if (criadoEm == null) {
            criadoEm = agora;
        }
        if (atualizadoEm == null) {
            atualizadoEm = agora;
        }
    }

    void aoAtualizar(Instant agora) {
        atualizadoEm = agora;
    }

    public Instant getArquivadoEm() { return arquivadoEm; }
    public Instant getExcluidoEm() { return excluidoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setArquivadoEm(Instant arquivadoEm) { this.arquivadoEm = arquivadoEm; }
    public void setExcluidoEm(Instant excluidoEm) { this.excluidoEm = excluidoEm; }
}
