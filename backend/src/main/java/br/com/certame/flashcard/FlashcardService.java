package br.com.certame.flashcard;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.deck.Deck;
import br.com.certame.flashcard.deck.DeckRepository;
import br.com.certame.flashcard.dto.FlashcardResumo;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import br.com.certame.flashcard.dto.RespostaFlashcardCriado;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
public class FlashcardService {
    private static final int MIN_TAMANHO = 3;
    private static final int MAX_FRENTE = 1000;
    private static final int MAX_VERSO = 4000;
    private final FlashcardRepository flashcards;
    private final DeckRepository decks;
    private final VerificadorDonoFlashcard verificador;
    private final ResolvedorContextoFlashcard resolvedor;
    private final AplicadorOrigemFlashcard aplicadorOrigem;
    private final NormalizadorTags normalizador;
    private final EntityManager entityManager;
    private final Clock clock;

    public FlashcardService(FlashcardRepository flashcards, DeckRepository decks,
            VerificadorDonoFlashcard verificador, ResolvedorContextoFlashcard resolvedor,
            AplicadorOrigemFlashcard aplicadorOrigem, NormalizadorTags normalizador,
            EntityManager entityManager, Clock clock) {
        this.flashcards = flashcards;
        this.decks = decks;
        this.verificador = verificador;
        this.resolvedor = resolvedor;
        this.aplicadorOrigem = aplicadorOrigem;
        this.normalizador = normalizador;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public RespostaFlashcardCriado criar(UUID usuarioId, RequisicaoFlashcard requisicao) {
        var textos = validarTextos(requisicao);
        var contexto = resolvedor.resolver(usuarioId, requisicao);
        var card = new Flashcard(entityManager.getReference(Usuario.class, usuarioId), textos.get(0), textos.get(1));
        card.setTags(normalizador.normalizar(requisicao.tags()).toArray(String[]::new));
        card.setDeck(deckDoUsuario(requisicao.deckId(), usuarioId));
        aplicadorOrigem.aplicar(card, contexto, requisicao);
        card.agendarPara(Instant.now(clock));
        var criado = flashcards.save(card);
        var total = contexto.sessao() == null ? null
                : flashcards.countByUsuarioIdAndOrigemSessaoOrigemId(usuarioId, contexto.sessao().getId());
        return new RespostaFlashcardCriado(FlashcardResumo.de(criado), total);
    }

    @Transactional
    public FlashcardResumo atualizar(UUID id, UUID usuarioId, RequisicaoFlashcard requisicao) {
        var card = verificador.exigirProprio(id, usuarioId);
        var textos = validarTextos(requisicao);
        card.setFrente(textos.get(0));
        card.setVerso(textos.get(1));
        card.setDeck(deckDoUsuario(requisicao.deckId(), usuarioId));
        card.setTags(normalizador.normalizar(requisicao.tags()).toArray(String[]::new));
        if (card.getOrigem().getQuestaoOrigem() == null) {
            aplicadorOrigem.aplicarManual(card, requisicao);
        }
        return FlashcardResumo.de(card);
    }

    private List<String> validarTextos(RequisicaoFlashcard requisicao) {
        var erros = new java.util.LinkedHashMap<String, String>();
        var frente = requisicao.frente() == null ? "" : requisicao.frente().trim();
        var verso = requisicao.verso() == null ? "" : requisicao.verso().trim();
        if (frente.length() < MIN_TAMANHO || frente.length() > MAX_FRENTE) erros.put("frente", "A frente deve ter entre 3 e 1000 caracteres.");
        if (verso.length() < MIN_TAMANHO || verso.length() > MAX_VERSO) erros.put("verso", "O verso deve ter entre 3 e 4000 caracteres.");
        if (!erros.isEmpty()) throw new DadosInvalidosException(erros);
        return List.of(frente, verso);
    }

    private Deck deckDoUsuario(UUID deckId, UUID usuarioId) {
        if (deckId == null) return null;
        return decks.findByIdAndUsuarioId(deckId, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }
}
