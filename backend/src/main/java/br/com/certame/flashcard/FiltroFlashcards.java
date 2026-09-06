package br.com.certame.flashcard;

import java.util.UUID;

public record FiltroFlashcards(String termo, String deckId, UUID disciplinaId, EstadoFlashcard estado,
        boolean arquivados) {
    public static final String SEM_DECK = "sem-deck";

    public boolean temTermo() { return termo != null && !termo.isBlank(); }
}
