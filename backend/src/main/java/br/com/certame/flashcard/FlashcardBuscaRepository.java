package br.com.certame.flashcard;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FlashcardBuscaRepository {
    Page<Flashcard> buscar(UUID usuarioId, FiltroFlashcards filtro, StatusFlashcard status, Pageable pageable);
}
