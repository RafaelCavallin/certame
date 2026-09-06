package br.com.certame.flashcard.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.deck.dto.DeckDetalhe;
import br.com.certame.flashcard.dto.RequisicaoDeck;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.certame.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T14:02:11Z");
    @Mock DeckRepository decks;
    @Mock FlashcardRepository flashcards;
    @Mock EntityManager entityManager;
    @Mock PainelDecksService painel;
    private final UUID usuarioId = UUID.randomUUID();
    private DeckService service;

    @BeforeEach
    void preparar() {
        service = new DeckService(decks, flashcards, entityManager, Clock.fixed(AGORA, ZoneOffset.UTC), painel);
    }

    @Test
    void TU28_recusaNomeRepetidoIgnorandoMaiusculas() {
        when(decks.existePorNomeIgnorandoCaixa(usuarioId, "Direito Constitucional")).thenReturn(true);

        var erro = assertThatThrownBy(() -> service.criar(usuarioId, new RequisicaoDeck(" Direito Constitucional ")))
                .isInstanceOf(ConflitoException.class).actual();
        assertThat(((ConflitoException) erro).getFieldErrors()).containsKey("nome");
        verify(decks, never()).saveAndFlush(any());
    }

    @Test
    void TU29_recusaNomeComDoisCaracteres() {
        var erro = assertThatThrownBy(() -> service.criar(usuarioId, new RequisicaoDeck("ab")))
                .isInstanceOf(DadosInvalidosException.class).actual();
        assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("nome");
        verifyNoInteractions(decks);
    }

    @Test
    void TU31_criarAplicaDescricaoELimitesInformadosNaMesmaChamada() {
        var usuario = mock(Usuario.class);
        when(entityManager.getReference(Usuario.class, usuarioId)).thenReturn(usuario);
        var captor = ArgumentCaptor.forClass(Deck.class);
        when(decks.saveAndFlush(captor.capture())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var requisicao = new RequisicaoDeck("Deck com limites", "Descrição do deck", (short) 20, (short) 100,
                (short) 30, new BigDecimal("0.85"));
        service.criar(usuarioId, requisicao);

        var salvo = captor.getValue();
        assertThat(salvo.getDescricao()).isEqualTo("Descrição do deck");
        assertThat(salvo.getNovosDia()).isEqualTo((short) 20);
        assertThat(salvo.getRevisoesDia()).isEqualTo((short) 100);
        assertThat(salvo.getIntervaloMaxDias()).isEqualTo((short) 30);
        assertThat(salvo.getRetencaoAlvo()).isEqualByComparingTo("0.85");
    }

    @Test
    void TU30_moverIgnoraIdsDeOutroUsuarioComMovidosRefletindoONumeroReal() {
        var deckId = UUID.randomUUID();
        var deck = mock(Deck.class);
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));
        when(flashcards.moverParaDeck(eq(deck), eq(ids), eq(usuarioId), any())).thenReturn(2);

        var movidos = service.mover(deckId, usuarioId, ids);

        assertThat(movidos).isEqualTo(2);
    }

    @Test
    void TU25_atualizarRecusaNomeComMenosDeTresCaracteres() {
        var deckId = UUID.randomUUID();
        var deck = umDeck(deckId);
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));

        var erro = assertThatThrownBy(
                () -> service.atualizar(deckId, usuarioId, new RequisicaoDeck("ab", null, null, null, null, null)))
                .isInstanceOf(DadosInvalidosException.class).actual();

        assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("nome");
        verify(decks, never()).existePorNomeIgnorandoCaixaExcluindo(any(), any(), any());
    }

    @Test
    void TU26_atualizarRecusaNomeJaUsadoPorOutroDeck() {
        var deckId = UUID.randomUUID();
        var deck = umDeck(deckId);
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));
        when(decks.existePorNomeIgnorandoCaixaExcluindo(usuarioId, "Direito Administrativo", deckId)).thenReturn(true);

        var erro = assertThatThrownBy(() -> service.atualizar(deckId, usuarioId,
                new RequisicaoDeck("Direito Administrativo", null, null, null, null, null)))
                .isInstanceOf(ConflitoException.class).actual();
        assertThat(((ConflitoException) erro).getFieldErrors()).containsKey("nome");
    }

    @Test
    void TU27_atualizarAceitaOProprioNomeSemAlteracao() {
        var deckId = UUID.randomUUID();
        var deck = umDeck(deckId);
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));
        when(decks.existePorNomeIgnorandoCaixaExcluindo(usuarioId, "Direito Constitucional", deckId)).thenReturn(false);
        when(painel.detalheDe(deck)).thenReturn(umDetalhe(deckId));

        var detalhe = service.atualizar(deckId, usuarioId,
                new RequisicaoDeck("Direito Constitucional", null, null, null, null, null));

        assertThat(detalhe.id()).isEqualTo(deckId);
        assertThat(deck.getNome()).isEqualTo("Direito Constitucional");
    }

    @Test
    void TU28_atualizarNaoEscreveEmNenhumFlashcard() {
        var deckId = UUID.randomUUID();
        var deck = umDeck(deckId);
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));
        when(painel.detalheDe(deck)).thenReturn(umDetalhe(deckId));

        service.atualizar(deckId, usuarioId,
                new RequisicaoDeck("Constitucional — controle", "ADI e ADC", (short) 15, (short) 80, (short) 200,
                        new BigDecimal("0.95")));

        verifyNoInteractions(flashcards);
        assertThat(deck.getNovosDia()).isEqualTo((short) 15);
        assertThat(deck.getRetencaoAlvo()).isEqualByComparingTo("0.95");
    }

    @Test
    void TU29_excluirMoveOsCardsENaoApagaNenhum() {
        var deckId = UUID.randomUUID();
        var deck = umDeck(deckId);
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.of(deck));
        when(flashcards.removerDeckDosCards(eq(deckId), eq(usuarioId), any())).thenReturn(4);

        var movidos = service.excluir(deckId, usuarioId);

        assertThat(movidos).isEqualTo(4);
        verify(decks).delete(deck);
    }

    @Test
    void TU30_excluirRecusaDeckDeOutroUsuario() {
        var deckId = UUID.randomUUID();
        when(decks.findByIdAndUsuarioId(deckId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(deckId, usuarioId)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(flashcards);
    }

    private Deck umDeck(UUID deckId) {
        var deck = new Deck(mock(br.com.certame.usuario.Usuario.class), "Direito Constitucional");
        setId(deck, deckId);
        return deck;
    }

    private void setId(Deck deck, UUID deckId) {
        try {
            var campo = Deck.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(deck, deckId);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private DeckDetalhe umDetalhe(UUID deckId) {
        return new DeckDetalhe(deckId, "Direito Constitucional", null, 0, 0, 0, LimitesDeck.PADRAO,
                LimitesDeck.PADRAO.rotuloLimites(), LimitesDeck.PADRAO.rotuloRetencao(), true, null);
    }
}
