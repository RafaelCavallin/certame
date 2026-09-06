package br.com.certame.flashcard.revisao.dto;

import java.util.List;
import java.util.UUID;

/** Corpo de `GET /flashcards/resumo` — alimenta o contador do shell e o Início. */
public record ResumoFlashcards(long vencendoHoje, long proximos7Dias, long novosDisponiveis,
        List<ResumoDeck> porDeck) {

    public record ResumoDeck(UUID deckId, String nome, long vencendoHoje, long novos) {
    }
}
