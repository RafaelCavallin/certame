package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.EstadoFlashcard;
import java.time.Instant;

/** Saída de `AgendadorFsrs.proximo` (CA-07): a memória recalculada e o próximo vencimento. */
public record Agendamento(double estabilidade, double dificuldade, EstadoFlashcard estado, int passoAprendizado,
        int intervaloDias, int intervaloMinutos, Instant proximaRevisaoEm) {
}
