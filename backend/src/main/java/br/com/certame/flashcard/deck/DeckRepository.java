package br.com.certame.flashcard.deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    Optional<Deck> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Deck> findByUsuarioIdOrderByNomeAsc(UUID usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(UUID usuarioId, String nome);

    @Query("select case when count(d) > 0 then true else false end from Deck d where d.usuario.id = :usuarioId and lower(d.nome) = lower(:nome)")
    boolean existePorNomeIgnorandoCaixa(@Param("usuarioId") UUID usuarioId, @Param("nome") String nome);

    @Query("select case when count(d) > 0 then true else false end from Deck d"
            + " where d.usuario.id = :usuarioId and lower(d.nome) = lower(:nome) and d.id <> :id")
    boolean existePorNomeIgnorandoCaixaExcluindo(@Param("usuarioId") UUID usuarioId, @Param("nome") String nome, @Param("id") UUID id);

    @Query(value = """
            select d.id as deck_id, d.nome as nome, d.descricao as descricao,
                   count(f.id) as total_cards
              from deck d
              left join flashcard f on f.deck_id = d.id and f.status <> 'EXCLUIDO'
             where d.usuario_id = :usuarioId
             group by d.id, d.nome, d.descricao
             order by d.nome asc
            """, nativeQuery = true)
    List<DeckComContagem> listarComContagem(@Param("usuarioId") UUID usuarioId);

    interface DeckComContagem {
        UUID getDeckId();
        String getNome();
        String getDescricao();
        long getTotalCards();
    }
}
