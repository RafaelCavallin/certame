package br.com.certame.flashcard.revisao;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.deck.Deck;
import br.com.certame.flashcard.deck.DeckRepository;
import br.com.certame.flashcard.revisao.dto.RequisicaoSessaoRevisao;
import br.com.certame.flashcard.revisao.dto.ResumoSessao;
import br.com.certame.flashcard.revisao.dto.RespostaSessaoRevisao;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-33/RF-34: abre a sessão com a fila montada e finaliza com o resumo consolidado. */
@Service
@PreAuthorize("isAuthenticated()")
public class SessaoRevisaoService {
    private final SessaoRevisaoRepository sessoes;
    private final FlashcardRevisaoRepository registros;
    private final DeckRepository decks;
    private final MontadorFila montador;
    private final ConstrutorCardRevisao construtorCard;
    private final ResumoRevisaoService resumo;
    private final RelogioEstudo relogio;
    private final EntityManager entityManager;
    private final LocalizadorSessaoRevisao localizadorSessao;
    private final Clock clock;

    public SessaoRevisaoService(SessaoRevisaoRepository sessoes, FlashcardRevisaoRepository registros,
            DeckRepository decks, MontadorFila montador, ConstrutorCardRevisao construtorCard,
            ResumoRevisaoService resumo, RelogioEstudo relogio, EntityManager entityManager,
            LocalizadorSessaoRevisao localizadorSessao, Clock clock) {
        this.sessoes = sessoes;
        this.registros = registros;
        this.decks = decks;
        this.montador = montador;
        this.construtorCard = construtorCard;
        this.resumo = resumo;
        this.relogio = relogio;
        this.entityManager = entityManager;
        this.localizadorSessao = localizadorSessao;
        this.clock = clock;
    }

    @Transactional
    public RespostaSessaoRevisao abrir(UUID usuarioId, RequisicaoSessaoRevisao requisicao) {
        validarRequisicao(requisicao);
        var deck = deckDe(requisicao, usuarioId);
        var disciplina = referencia(Disciplina.class, requisicao.disciplinaId());
        var usuario = entityManager.getReference(Usuario.class, usuarioId);
        var sessao = sessoes.save(new SessaoRevisao(usuario, requisicao.escopo().name(), deck, disciplina));
        var filtro = new MontadorFila.FiltroFila(requisicao.escopo(), requisicao.deckId(), requisicao.disciplinaId());
        var fila = montador.montar(usuarioId, filtro, relogio).stream().map(construtorCard::de).toList();
        return new RespostaSessaoRevisao(sessao.getId(), sessao.getIniciadaEm(), escopoRotulo(requisicao.escopo(), deck),
                fila.size(), fila);
    }

    @Transactional
    public ResumoSessao finalizar(UUID sessaoId, UUID usuarioId) {
        var sessao = localizadorSessao.aberta(sessaoId, usuarioId);
        var consolidado = ConsolidadoSessao.de(registros.findBySessaoIdOrderByRevisadoEmAsc(sessaoId));
        sessao.finalizar(Instant.now(clock), (short) consolidado.revisados(), (short) consolidado.errei(),
                (short) consolidado.dificil(), (short) consolidado.bom(), (short) consolidado.facil(), consolidado.tempoMs());
        var distribuicao = new ResumoSessao.Distribuicao(consolidado.errei(), consolidado.dificil(), consolidado.bom(),
                consolidado.facil());
        return new ResumoSessao(consolidado.revisados(), consolidado.classificacoes(), distribuicao, consolidado.tempoMs(),
                ResumoSessao.rotuloTempo(consolidado.tempoMs()), resumo.proximosVencimentos(usuarioId));
    }

    private void validarRequisicao(RequisicaoSessaoRevisao requisicao) {
        if (requisicao.escopo() == EscopoFila.DECK && requisicao.deckId() == null) {
            throw new DadosInvalidosException(Map.of("deckId", "Informe o deck para revisar."));
        }
    }

    private Deck deckDe(RequisicaoSessaoRevisao requisicao, UUID usuarioId) {
        if (requisicao.escopo() != EscopoFila.DECK) {
            return null;
        }
        return decks.findByIdAndUsuarioId(requisicao.deckId(), usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }
    private <T> T referencia(Class<T> tipo, UUID id) {
        return id == null ? null : entityManager.getReference(tipo, id);
    }
    private static String escopoRotulo(EscopoFila escopo, Deck deck) {
        return switch (escopo) {
            case TODOS -> "Todos os decks";
            case SEM_DECK -> "Sem deck";
            case DECK -> deck.getNome();
        };
    }
}
