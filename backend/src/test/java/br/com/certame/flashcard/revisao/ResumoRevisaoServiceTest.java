package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.StatusFlashcard;
import br.com.certame.flashcard.deck.Deck;
import br.com.certame.flashcard.deck.DeckRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumoRevisaoServiceTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 4);
    @Mock FlashcardRepository flashcards;
    @Mock DeckRepository decks;
    @Mock RelogioEstudo relogio;
    private final UUID usuarioId = UUID.randomUUID();
    private ResumoRevisaoService service;

    @BeforeEach
    void preparar() {
        service = new ResumoRevisaoService(flashcards, decks, relogio);
        when(relogio.hoje()).thenReturn(HOJE);
    }

    @Test
    void TU48_montaAsTresFaixasDeVencimento() {
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(1), HOJE.plusDays(1))).thenReturn(6L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(2), HOJE.plusDays(7))).thenReturn(18L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoGreaterThan(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(7))).thenReturn(41L);

        var faixas = service.proximosVencimentos(usuarioId);

        assertThat(faixas).hasSize(3);
        assertThat(faixas.get(0).faixa()).isEqualTo("AMANHA");
        assertThat(faixas.get(0).quantidade()).isEqualTo(6);
        assertThat(faixas.get(1).faixa()).isEqualTo("PROXIMOS_7_DIAS");
        assertThat(faixas.get(1).quantidade()).isEqualTo(18);
        assertThat(faixas.get(2).faixa()).isEqualTo("DEPOIS_DE_7_DIAS");
        assertThat(faixas.get(2).quantidade()).isEqualTo(41);
    }

    @Test
    void TU48b_devolveZeroQuandoNaoHaCardsNaFaixa() {
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(1), HOJE.plusDays(1))).thenReturn(0L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(2), HOJE.plusDays(7))).thenReturn(0L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoGreaterThan(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(7))).thenReturn(0L);

        var faixas = service.proximosVencimentos(usuarioId);

        assertThat(faixas).extracting("quantidade").containsExactly(0L, 0L, 0L);
    }

    @Test
    void TU49_listaTodosOsDecksComContagemENomeDoSemDeck() {
        var deckId = UUID.randomUUID();
        var deck = mock(Deck.class);
        when(deck.getId()).thenReturn(deckId);
        when(deck.getNome()).thenReturn("Constitucional");
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of(deck));
        var contagemDeck = contagem(deckId, 5, 2);
        var contagemSemDeck = contagem(null, 3, 1);
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of(contagemDeck, contagemSemDeck));
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(usuarioId, StatusFlashcard.ATIVO, HOJE))
                .thenReturn(8L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoBetween(usuarioId, StatusFlashcard.ATIVO,
                HOJE.plusDays(1), HOJE.plusDays(7))).thenReturn(4L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoEstadoIn(usuarioId, StatusFlashcard.ATIVO,
                List.of(EstadoFlashcard.NOVO))).thenReturn(9L);

        var resumo = service.resumoDoUsuario(usuarioId);

        assertThat(resumo.vencendoHoje()).isEqualTo(8);
        assertThat(resumo.proximos7Dias()).isEqualTo(4);
        assertThat(resumo.novosDisponiveis()).isEqualTo(9);
        assertThat(resumo.porDeck()).hasSize(2);
        assertThat(resumo.porDeck().get(0).nome()).isEqualTo("Constitucional");
        assertThat(resumo.porDeck().get(1).nome()).isEqualTo("Sem deck");
    }

    private FlashcardRepository.ContagemPorDeck contagem(UUID deckId, long vencendoHoje, long novos) {
        return new FlashcardRepository.ContagemPorDeck() {
            public UUID getDeckId() { return deckId; }
            public long getTotal() { return vencendoHoje + novos; }
            public long getVencendoHoje() { return vencendoHoje; }
            public long getNovos() { return novos; }
        };
    }
}
