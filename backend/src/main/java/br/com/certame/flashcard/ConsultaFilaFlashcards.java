package br.com.certame.flashcard;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
public class ConsultaFilaFlashcards {
    private final FlashcardRepository flashcards;

    public ConsultaFilaFlashcards(FlashcardRepository flashcards) {
        this.flashcards = flashcards;
    }

    @Transactional(readOnly = true)
    public List<Flashcard> elegiveis(UUID usuarioId, LocalDate ate) {
        return flashcards.buscarElegiveis(usuarioId, ate, StatusFlashcard.ATIVO);
    }
}
