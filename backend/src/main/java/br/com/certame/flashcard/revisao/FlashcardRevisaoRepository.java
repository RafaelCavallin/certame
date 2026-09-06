package br.com.certame.flashcard.revisao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRevisaoRepository extends JpaRepository<FlashcardRevisao, UUID> {

    List<FlashcardRevisao> findByFlashcardIdOrderByRevisadoEmDesc(UUID flashcardId);

    List<FlashcardRevisao> findBySessaoIdOrderByRevisadoEmAsc(UUID sessaoId);
}
