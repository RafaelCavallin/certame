package br.com.certame.flashcard;

import br.com.certame.flashcard.deck.Deck;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashcardRepository
        extends JpaRepository<Flashcard, UUID>, FlashcardBuscaRepository, FlashcardContagemRepository {

    Optional<Flashcard> findByIdAndUsuarioIdAndStatusNot(UUID id, UUID usuarioId, StatusFlashcard status);

    Optional<Flashcard> findByIdAndUsuarioIdAndStatus(UUID id, UUID usuarioId, StatusFlashcard status);

    List<Flashcard> findByUsuarioIdAndStatus(UUID usuarioId, StatusFlashcard status);

    Page<Flashcard> findByUsuarioIdAndStatusNot(UUID usuarioId, StatusFlashcard status, Pageable pageable);

    Page<Flashcard> findByUsuarioIdAndDeckIdAndStatusNot(UUID usuarioId, UUID deckId, StatusFlashcard status, Pageable pageable);

    @Query(value = """
            select * from flashcard
             where usuario_id = :usuarioId
               and status <> 'EXCLUIDO'
               and busca @@ websearch_to_tsquery('portuguese', :termo)
            """, nativeQuery = true)
    List<Flashcard> buscarPorTermo(@Param("usuarioId") UUID usuarioId, @Param("termo") String termo);

    long countByUsuarioIdAndStatus(UUID usuarioId, StatusFlashcard status);

    long countByUsuarioIdAndStatusNot(UUID usuarioId, StatusFlashcard status);

    long countByUsuarioIdAndOrigemSessaoOrigemId(UUID usuarioId, UUID sessaoOrigemId);

    @Query("select f from Flashcard f where f.usuario.id = :usuarioId and f.status = :status"
            + "   and f.agendamento.proximaRevisao <= :ate")
    List<Flashcard> buscarElegiveis(@Param("usuarioId") UUID usuarioId, @Param("ate") LocalDate ate, @Param("status") StatusFlashcard status);

    @Query("select f from Flashcard f left join fetch f.deck"
            + " where f.usuario.id = :usuarioId and f.status = br.com.certame.flashcard.StatusFlashcard.ATIVO"
            + "   and f.agendamento.proximaRevisao <= :ate"
            + "   and (:escopo = 'TODOS'"
            + "        or (:escopo = 'DECK' and f.deck.id = :deckId)"
            + "        or (:escopo = 'SEM_DECK' and f.deck is null))"
            + "   and (:disciplinaId is null or f.origem.disciplina.id = :disciplinaId)"
            + " order by f.agendamento.proximaRevisaoEm asc, f.registroTemporal.criadoEm asc")
    List<Flashcard> buscarFilaPorEscopo(@Param("usuarioId") UUID usuarioId, @Param("ate") LocalDate ate,
            @Param("escopo") String escopo, @Param("deckId") UUID deckId, @Param("disciplinaId") UUID disciplinaId);

    long countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(UUID usuarioId, StatusFlashcard status, LocalDate ate);

    long countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(UUID usuarioId, StatusFlashcard status, LocalDate de, LocalDate ate);

    long countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoGreaterThan(UUID usuarioId, StatusFlashcard status, LocalDate data);

    long countByUsuarioIdAndStatusAndAgendamentoEstadoIn(UUID usuarioId, StatusFlashcard status, Collection<EstadoFlashcard> estados);

    @Query("select count(distinct f.deck.id) from Flashcard f where f.usuario.id = :usuarioId and f.status = :status and f.deck is not null")
    long contarDecksComCardAtivo(@Param("usuarioId") UUID usuarioId, @Param("status") StatusFlashcard status);

    @Modifying(clearAutomatically = true)
    @Query("update Flashcard f set f.deck = :deck, f.registroTemporal.atualizadoEm = :agora"
            + " where f.id in :ids and f.usuario.id = :usuarioId and f.status <> br.com.certame.flashcard.StatusFlashcard.EXCLUIDO")
    int moverParaDeck(@Param("deck") Deck deck, @Param("ids") List<UUID> ids, @Param("usuarioId") UUID usuarioId,
            @Param("agora") Instant agora);

    @Modifying(clearAutomatically = true)
    @Query("update Flashcard f set f.deck = null, f.registroTemporal.atualizadoEm = :agora"
            + " where f.deck.id = :deckId and f.usuario.id = :usuarioId and f.status <> br.com.certame.flashcard.StatusFlashcard.EXCLUIDO")
    int removerDeckDosCards(@Param("deckId") UUID deckId, @Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);
}
