package br.com.certame.flashcard;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Contagens por deck (total, vencendo hoje, novos) usadas no painel de decks. */
public interface FlashcardContagemRepository {

    @Query(value = """
            select f.deck_id as deck_id,
                   count(*) filter (where f.status = 'ATIVO') as total,
                   count(*) filter (where f.status = 'ATIVO' and f.proxima_revisao <= :hoje) as vencendo_hoje,
                   count(*) filter (where f.status = 'ATIVO' and f.estado = 'NOVO') as novos
              from flashcard f
             where f.usuario_id = :usuarioId
             group by f.deck_id
            """, nativeQuery = true)
    List<ContagemPorDeck> contarPorDeck(@Param("usuarioId") UUID usuarioId, @Param("hoje") LocalDate hoje);

    @Query(value = """
            select count(*) filter (where f.status = 'ATIVO') as total,
                   count(*) filter (where f.status = 'ATIVO' and f.proxima_revisao <= :hoje) as vencendo_hoje,
                   count(*) filter (where f.status = 'ATIVO' and f.estado = 'NOVO') as novos
              from flashcard f
             where f.usuario_id = :usuarioId and f.deck_id = :deckId
            """, nativeQuery = true)
    ContagemPorDeck contarDeUmDeck(@Param("usuarioId") UUID usuarioId, @Param("deckId") UUID deckId, @Param("hoje") LocalDate hoje);

    interface ContagemPorDeck {
        UUID getDeckId();
        long getTotal();
        long getVencendoHoje();
        long getNovos();
    }
}
