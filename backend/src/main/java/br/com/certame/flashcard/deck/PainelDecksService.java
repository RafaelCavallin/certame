package br.com.certame.flashcard.deck;

import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.FlashcardContagemRepository.ContagemPorDeck;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.deck.dto.AvisoDeck;
import br.com.certame.flashcard.deck.dto.DeckDetalhe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
public class PainelDecksService {
    private static final int MULTIPLICADOR_AVISO = 2;
    private final DeckRepository decks;
    private final FlashcardRepository flashcards;
    private final RelogioEstudo relogio;

    public PainelDecksService(DeckRepository decks, FlashcardRepository flashcards, RelogioEstudo relogio) {
        this.decks = decks;
        this.flashcards = flashcards;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<DeckDetalhe> painel(UUID usuarioId) {
        var contagens = indexarPorDeck(flashcards.contarPorDeck(usuarioId, relogio.hoje()));
        var detalhes = new ArrayList<DeckDetalhe>();
        decks.findByUsuarioIdOrderByNomeAsc(usuarioId)
                .forEach(deck -> detalhes.add(detalheReal(deck, contagens.get(deck.getId()))));
        var semDeck = contagens.get(null);
        if (semDeck != null && semDeck.getTotal() > 0) {
            detalhes.add(detalheSemDeck(semDeck));
        }
        return detalhes;
    }

    @Transactional(readOnly = true)
    public DeckDetalhe detalheDe(Deck deck) {
        return detalheReal(deck, flashcards.contarDeUmDeck(deck.getUsuario().getId(), deck.getId(), relogio.hoje()));
    }

    private static Map<UUID, ContagemPorDeck> indexarPorDeck(List<ContagemPorDeck> contagens) {
        return contagens.stream().collect(Collectors.toMap(ContagemPorDeck::getDeckId, c -> c));
    }

    private DeckDetalhe detalheReal(Deck deck, ContagemPorDeck contagem) {
        var limites = LimitesDeck.de(deck);
        var vencendoHoje = vencendoHoje(contagem);
        return new DeckDetalhe(deck.getId(), deck.getNome(), deck.getDescricao(), total(contagem), vencendoHoje,
                novos(contagem), limites, limites.rotuloLimites(), limites.rotuloRetencao(), true,
                avisoDe(vencendoHoje, limites));
    }

    private DeckDetalhe detalheSemDeck(ContagemPorDeck contagem) {
        var limites = LimitesDeck.PADRAO;
        var vencendoHoje = vencendoHoje(contagem);
        return new DeckDetalhe(null, "Sem deck", null, total(contagem), vencendoHoje, novos(contagem), limites,
                limites.rotuloLimites(), limites.rotuloRetencao(), false, avisoDe(vencendoHoje, limites));
    }

    private AvisoDeck avisoDe(long vencendoHoje, LimitesDeck limites) {
        var limiteDiario = limites.revisoesDia();
        if (vencendoHoje <= (long) MULTIPLICADOR_AVISO * limiteDiario) {
            return null;
        }
        var mensagem = ("%d cards vencidos, mais que o dobro do limite diário deste deck. "
                + "Ajuste os limites para retomar no seu ritmo.").formatted(vencendoHoje);
        return new AvisoDeck(AvisoDeck.FILA_ACUMULADA, mensagem, vencendoHoje, limiteDiario);
    }

    private static long total(ContagemPorDeck contagem) { return contagem == null ? 0 : contagem.getTotal(); }
    private static long vencendoHoje(ContagemPorDeck contagem) { return contagem == null ? 0 : contagem.getVencendoHoje(); }
    private static long novos(ContagemPorDeck contagem) { return contagem == null ? 0 : contagem.getNovos(); }
}
