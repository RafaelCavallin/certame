package br.com.certame.flashcard.dto;

import java.util.List;
import java.util.UUID;

public record RequisicaoFlashcard(
        String frente,
        String verso,
        UUID deckId,
        UUID disciplinaId,
        UUID assuntoId,
        List<String> tags,
        UUID questaoOrigemId,
        UUID sessaoOrigemId) {
}
