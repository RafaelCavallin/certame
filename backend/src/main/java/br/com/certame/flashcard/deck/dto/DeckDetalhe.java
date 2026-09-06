package br.com.certame.flashcard.deck.dto;

import br.com.certame.flashcard.deck.LimitesDeck;
import java.util.UUID;

public record DeckDetalhe(
        UUID id,
        String nome,
        String descricao,
        long totalCards,
        long vencendoHoje,
        long novos,
        LimitesDeck limites,
        String limitesRotulo,
        String retencaoRotulo,
        boolean editavel,
        AvisoDeck aviso) {
}
