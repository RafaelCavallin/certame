package br.com.certame.flashcard.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.FlashcardContagemRepository.ContagemPorDeck;
import br.com.certame.flashcard.RelogioEstudo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PainelDecksServiceTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 4);
    @Mock DeckRepository decks;
    @Mock FlashcardRepository flashcards;
    @Mock RelogioEstudo relogio;
    private final UUID usuarioId = UUID.randomUUID();
    private PainelDecksService service;

    @BeforeEach
    void preparar() {
        service = new PainelDecksService(decks, flashcards, relogio);
        when(relogio.hoje()).thenReturn(HOJE);
    }

    @Test
    void TU49_calculaTotalVencendoHojeENovos() {
        var deck = umDeck(new java.math.BigDecimal("0.90"));
        var contagem = contagem(deck.getId(), 20, 5, 8);
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of(deck));
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of(contagem));

        var painel = service.painel(usuarioId);

        assertThat(painel).hasSize(1);
        assertThat(painel.getFirst().totalCards()).isEqualTo(20);
        assertThat(painel.getFirst().vencendoHoje()).isEqualTo(5);
        assertThat(painel.getFirst().novos()).isEqualTo(8);
    }

    @Test
    void TU50_emiteAvisoAcimaDoDobroDoLimite() {
        var deck = umDeck(new java.math.BigDecimal("0.90"));
        var contagem = contagem(deck.getId(), 214, 137, 12);
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of(deck));
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of(contagem));

        var painel = service.painel(usuarioId);

        assertThat(painel.getFirst().aviso()).isNotNull();
        assertThat(painel.getFirst().aviso().vencidos()).isEqualTo(137);
        assertThat(painel.getFirst().aviso().limiteDiario()).isEqualTo(60);
    }

    @Test
    void TU51_naoEmiteAvisoNoDobroExato() {
        var deck = umDeck(new java.math.BigDecimal("0.90"));
        var contagem = contagem(deck.getId(), 120, 120, 0);
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of(deck));
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of(contagem));

        var painel = service.painel(usuarioId);

        assertThat(painel.getFirst().aviso()).isNull();
    }

    @Test
    void TU52_incluiSemDeckApenasQuandoHaCards() {
        var contagem = contagem(null, 3, 1, 3);
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of());
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of(contagem));

        var painel = service.painel(usuarioId);

        assertThat(painel).hasSize(1);
        assertThat(painel.getFirst().id()).isNull();
        assertThat(painel.getFirst().nome()).isEqualTo("Sem deck");
        assertThat(painel.getFirst().editavel()).isFalse();
    }

    @Test
    void TU52_omiteSemDeckQuandoNaoHaCardsSemDeck() {
        when(decks.findByUsuarioIdOrderByNomeAsc(usuarioId)).thenReturn(List.of());
        when(flashcards.contarPorDeck(usuarioId, HOJE)).thenReturn(List.of());

        var painel = service.painel(usuarioId);

        assertThat(painel).isEmpty();
    }

    private Deck umDeck(java.math.BigDecimal retencaoAlvo) {
        var deck = new Deck(mock(br.com.certame.usuario.Usuario.class), "Constitucional — controle");
        setId(deck, UUID.randomUUID());
        deck.setRetencaoAlvo(retencaoAlvo);
        return deck;
    }

    private void setId(Deck deck, UUID id) {
        try {
            var campo = Deck.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(deck, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ContagemPorDeck contagem(UUID deckId, long total, long vencendoHoje, long novos) {
        var contagem = mock(ContagemPorDeck.class);
        when(contagem.getDeckId()).thenReturn(deckId);
        when(contagem.getTotal()).thenReturn(total);
        when(contagem.getVencendoHoje()).thenReturn(vencendoHoje);
        when(contagem.getNovos()).thenReturn(novos);
        return contagem;
    }
}
