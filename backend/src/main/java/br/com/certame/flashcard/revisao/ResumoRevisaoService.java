package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.StatusFlashcard;
import br.com.certame.flashcard.deck.Deck;
import br.com.certame.flashcard.deck.DeckRepository;
import br.com.certame.flashcard.revisao.dto.FaixaVencimento;
import br.com.certame.flashcard.revisao.dto.ResumoFlashcards;
import br.com.certame.flashcard.revisao.dto.ResumoFlashcards.ResumoDeck;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-34 e `GET /flashcards/resumo`: distribuição, tempo e faixas de vencimento futuro — somente leitura. */
@Service
@PreAuthorize("isAuthenticated()")
public class ResumoRevisaoService {
    private final FlashcardRepository flashcards;
    private final DeckRepository decks;
    private final RelogioEstudo relogio;

    public ResumoRevisaoService(FlashcardRepository flashcards, DeckRepository decks, RelogioEstudo relogio) {
        this.flashcards = flashcards;
        this.decks = decks;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public ResumoFlashcards resumoDoUsuario(UUID usuarioId) {
        var hoje = relogio.hoje();
        var vencendoHoje = flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(usuarioId, StatusFlashcard.ATIVO, hoje);
        var proximos7Dias = contarEntre(usuarioId, hoje.plusDays(1), hoje.plusDays(7));
        var novosDisponiveis = flashcards.countByUsuarioIdAndStatusAndAgendamentoEstadoIn(usuarioId, StatusFlashcard.ATIVO, List.of(EstadoFlashcard.NOVO));
        return new ResumoFlashcards(vencendoHoje, proximos7Dias, novosDisponiveis, porDeck(usuarioId, hoje));
    }

    @Transactional(readOnly = true)
    public List<FaixaVencimento> proximosVencimentos(UUID usuarioId) {
        var hoje = relogio.hoje();
        var amanha = contarEntre(usuarioId, hoje.plusDays(1), hoje.plusDays(1));
        var proximos7 = contarEntre(usuarioId, hoje.plusDays(2), hoje.plusDays(7));
        var depois = flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoGreaterThan(usuarioId, StatusFlashcard.ATIVO, hoje.plusDays(7));
        return List.of(
                new FaixaVencimento(FaixaVencimento.AMANHA, "Amanhã", amanha),
                new FaixaVencimento(FaixaVencimento.PROXIMOS_7_DIAS, "Nos próximos 7 dias", proximos7),
                new FaixaVencimento(FaixaVencimento.DEPOIS_DE_7_DIAS, "Depois de 7 dias", depois));
    }

    private long contarEntre(UUID usuarioId, LocalDate de, LocalDate ate) {
        return flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO, de, ate);
    }

    private List<ResumoDeck> porDeck(UUID usuarioId, LocalDate hoje) {
        var nomes = decks.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .collect(Collectors.toMap(Deck::getId, Deck::getNome));
        return flashcards.contarPorDeck(usuarioId, hoje).stream()
                .map(c -> new ResumoDeck(c.getDeckId(), nomeDe(nomes, c.getDeckId()), c.getVencendoHoje(), c.getNovos()))
                .toList();
    }

    private static String nomeDe(Map<UUID, String> nomes, UUID deckId) {
        return deckId == null ? "Sem deck" : nomes.get(deckId);
    }
}
