package br.com.certame.flashcard;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VerificadorDonoFlashcard {
    private final FlashcardRepository flashcards;

    public VerificadorDonoFlashcard(FlashcardRepository flashcards) {
        this.flashcards = flashcards;
    }

    public Flashcard exigirProprio(UUID id, UUID usuarioId) {
        return flashcards.findByIdAndUsuarioIdAndStatusNot(id, usuarioId, StatusFlashcard.EXCLUIDO)
                .orElseThrow(RecursoNaoEncontradoException::new);
    }
}
