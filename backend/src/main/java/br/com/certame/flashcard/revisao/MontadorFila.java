package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.deck.LimitesDeck;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** RN-10: monta a fila deck a deck, descontando o consumo do dia e ordenando por vencimento. */
@Component
public class MontadorFila {

    private final FlashcardRepository flashcards;
    private final ConsumoDiarioRepositorio consumoDiario;

    public MontadorFila(FlashcardRepository flashcards, ConsumoDiarioRepositorio consumoDiario) {
        this.flashcards = flashcards;
        this.consumoDiario = consumoDiario;
    }

    public List<Flashcard> montar(UUID usuarioId, FiltroFila filtro, RelogioEstudo relogio) {
        var candidatos = flashcards.buscarFilaPorEscopo(usuarioId, relogio.hoje(), filtro.escopo().name(),
                filtro.deckId(), filtro.disciplinaId());
        var consumoPorDeck = consumoPorDeck(usuarioId, relogio);
        var porDeck = candidatos.stream()
                .collect(Collectors.groupingBy(MontadorFila::chaveDeck, LinkedHashMap::new, Collectors.toList()));
        var selecionados = new ArrayList<Flashcard>();
        porDeck.forEach((deckId, cards) -> selecionados.addAll(selecionarDoDeck(cards, consumoPorDeck.get(deckId))));
        return ordenarPorVencimento(selecionados);
    }

    private Map<Optional<UUID>, ConsumoDiario> consumoPorDeck(UUID usuarioId, RelogioEstudo relogio) {
        return consumoDiario.porDeck(usuarioId, relogio.inicioDoDia()).stream()
                .collect(Collectors.toMap(c -> Optional.ofNullable(c.deckId()), c -> c));
    }

    private static Optional<UUID> chaveDeck(Flashcard flashcard) {
        var deck = flashcard.getDeck();
        return deck == null ? Optional.empty() : Optional.of(deck.getId());
    }

    private List<Flashcard> selecionarDoDeck(List<Flashcard> cards, ConsumoDiario consumo) {
        var limites = LimitesDeck.de(cards.get(0).getDeck());
        var restanteNovos = restante(limites.novosDia(), consumo == null ? 0 : consumo.novos());
        var restanteRevisoes = restante(limites.revisoesDia(), consumo == null ? 0 : consumo.revisoes());
        var selecionados = new ArrayList<>(cards.stream().filter(MontadorFila::ehNovo).limit(restanteNovos).toList());
        selecionados.addAll(cards.stream().filter(f -> !ehNovo(f)).limit(restanteRevisoes).toList());
        return selecionados;
    }

    private static long restante(short limite, long consumido) {
        return Math.max(0, limite - consumido);
    }

    private static boolean ehNovo(Flashcard flashcard) {
        return flashcard.getAgendamento().getEstado() == EstadoFlashcard.NOVO;
    }

    private static List<Flashcard> ordenarPorVencimento(List<Flashcard> cards) {
        Comparator<Flashcard> porVencimento = Comparator.comparing(f -> f.getAgendamento().getProximaRevisaoEm());
        return cards.stream()
                .sorted(porVencimento.thenComparing(f -> f.getRegistroTemporal().getCriadoEm()))
                .toList();
    }

    public record FiltroFila(EscopoFila escopo, UUID deckId, UUID disciplinaId) {
    }
}
