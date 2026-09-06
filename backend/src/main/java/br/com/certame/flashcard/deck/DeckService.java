package br.com.certame.flashcard.deck;

import br.com.certame.comum.excecao.NomeDeckEmUsoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.deck.dto.DeckDetalhe;
import br.com.certame.flashcard.dto.DeckResumo;
import br.com.certame.flashcard.dto.RequisicaoDeck;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
public class DeckService {
    private static final int MAX_IDS_MOVER = 200;
    private final DeckRepository decks;
    private final FlashcardRepository flashcards;
    private final EntityManager entityManager;
    private final Clock clock;
    private final PainelDecksService painel;

    public DeckService(DeckRepository decks, FlashcardRepository flashcards, EntityManager entityManager, Clock clock,
            PainelDecksService painel) {
        this.decks = decks;
        this.flashcards = flashcards;
        this.entityManager = entityManager;
        this.clock = clock;
        this.painel = painel;
    }

    @Transactional(readOnly = true)
    public List<DeckDetalhe> listar(UUID usuarioId) {
        return painel.painel(usuarioId);
    }

    @Transactional
    public DeckResumo criar(UUID usuarioId, RequisicaoDeck requisicao) {
        var nome = ValidadorDeck.nome(requisicao.nome());
        if (decks.existePorNomeIgnorandoCaixa(usuarioId, nome)) {
            throw new NomeDeckEmUsoException();
        }
        try {
            var novoDeck = new Deck(entityManager.getReference(Usuario.class, usuarioId), nome,
                    ValidadorDeck.descricao(requisicao.descricao()));
            novoDeck.aplicarLimites(ValidadorDeck.limites(novoDeck, requisicao));
            var deck = decks.saveAndFlush(novoDeck);
            return new DeckResumo(deck.getId(), deck.getNome(), deck.getDescricao(), 0);
        } catch (DataIntegrityViolationException ex) {
            throw new NomeDeckEmUsoException();
        }
    }

    @Transactional
    public DeckDetalhe atualizar(UUID id, UUID usuarioId, RequisicaoDeck requisicao) {
        var deck = decks.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        var nome = ValidadorDeck.nome(requisicao.nome());
        if (decks.existePorNomeIgnorandoCaixaExcluindo(usuarioId, nome, id)) {
            throw new NomeDeckEmUsoException();
        }
        deck.setNome(nome);
        deck.setDescricao(ValidadorDeck.descricao(requisicao.descricao()));
        deck.aplicarLimites(ValidadorDeck.limites(deck, requisicao));
        return painel.detalheDe(deck);
    }

    @Transactional
    public int excluir(UUID id, UUID usuarioId) {
        var deck = decks.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        var movidos = flashcards.removerDeckDosCards(id, usuarioId, Instant.now(clock));
        decks.delete(deck);
        return movidos;
    }

    @Transactional
    public int mover(UUID deckId, UUID usuarioId, List<UUID> flashcardIds) {
        ValidadorDeck.listaFlashcards(flashcardIds, MAX_IDS_MOVER);
        var deck = decks.findByIdAndUsuarioId(deckId, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        return flashcards.moverParaDeck(deck, flashcardIds, usuarioId, Instant.now(clock));
    }
}
