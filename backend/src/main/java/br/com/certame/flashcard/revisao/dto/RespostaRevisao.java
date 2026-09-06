package br.com.certame.flashcard.revisao.dto;

import br.com.certame.flashcard.EstadoFlashcard;
import java.time.Instant;
import java.util.UUID;

public record RespostaRevisao(UUID flashcardId, EstadoFlashcard estado, double estabilidade, double dificuldade,
        int intervaloMinutos, String intervaloRotulo, Instant proximaRevisaoEm, boolean reenfileirar,
        CardRevisao card) {
}
