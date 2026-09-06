package br.com.certame.flashcard.revisao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Consumo do dia por deck (RN-10): um card com vários passos de aprendizado conta uma vaga só. */
public record ConsumoDiario(UUID deckId, long novos, long revisoes) {
}

/** Interface de topo: Spring Data não considera repositórios aninhados por padrão. */
interface ConsumoDiarioRepositorio extends Repository<FlashcardRevisao, UUID> {

    @Query(value = """
            select deck_id as "deckId",
                   count(*) filter (where houve_novo) as novos,
                   count(*) filter (where not houve_novo) as revisoes
              from (select f.deck_id as deck_id, fr.flashcard_id,
                           bool_or(fr.estabilidade_ant = 0) as houve_novo
                      from flashcard_revisao fr
                      join flashcard f on f.id = fr.flashcard_id
                     where fr.usuario_id = :usuarioId
                       and fr.revisado_em >= :inicioDoDia
                     group by f.deck_id, fr.flashcard_id) por_card
             group by deck_id
            """, nativeQuery = true)
    List<ConsumoDiario> porDeck(@Param("usuarioId") UUID usuarioId, @Param("inicioDoDia") Instant inicioDoDia);
}
