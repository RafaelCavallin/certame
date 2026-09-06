package br.com.certame.flashcard.dto;

import java.util.List;

public record PaginaFlashcards(List<FlashcardResumo> content, int page, int size, long totalElements,
        IndicadoresFlashcards indicadores) {
    public PaginaFlashcards {
        content = List.copyOf(content);
    }
}
