package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.certame.catalogo.Assunto;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.estudo.SessaoEstudo;
import br.com.certame.flashcard.deck.Deck;
import br.com.certame.flashcard.deck.DeckRepository;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import br.com.certame.questao.Questao;
import br.com.certame.usuario.Usuario;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T14:02:11Z");
    @Mock FlashcardRepository flashcards;
    @Mock DeckRepository decks;
    @Mock VerificadorDonoFlashcard verificador;
    @Mock ResolvedorContextoFlashcard resolvedor;
    @Mock EntityManager entityManager;
    private final NormalizadorTags normalizador = new NormalizadorTags();
    private final UUID usuarioId = UUID.randomUUID();
    private FlashcardService service;

    @BeforeEach
    void preparar() {
        service = new FlashcardService(flashcards, decks, verificador, resolvedor,
                new AplicadorOrigemFlashcard(entityManager), normalizador, entityManager,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        lenient().when(entityManager.getReference(Usuario.class, usuarioId)).thenReturn(mock(Usuario.class));
        lenient().when(flashcards.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void TU07_criaCardDeQuestaoComFsrsZeradoEDataDoClock() {
        var questao = mock(Questao.class);
        var sessao = mock(SessaoEstudo.class);
        when(resolvedor.resolver(eq(usuarioId), any())).thenReturn(new ContextoOrigem(questao, sessao));
        when(sessao.getId()).thenReturn(UUID.randomUUID());
        when(flashcards.countByUsuarioIdAndOrigemSessaoOrigemId(eq(usuarioId), any())).thenReturn(1L);
        var criado = service.criar(usuarioId, requisicao(" Frente ", " Verso ", null, UUID.randomUUID(), UUID.randomUUID()));
        assertThat(criado.flashcard().estado()).isEqualTo(EstadoFlashcard.NOVO);
        assertThat(criado.flashcard().proximaRevisao()).isEqualTo(AGORA.atZone(ZoneOffset.UTC).toLocalDate());
        assertThat(criado.criadosNaSessao()).isOne();
        verify(flashcards).save(any(Flashcard.class));
    }

    @Test
    void TU08_criaCardHerdandoTaxonomiaDaQuestao() {
        var questao = mock(Questao.class);
        var disciplina = mock(Disciplina.class);
        var assunto = mock(Assunto.class);
        when(questao.getDisciplina()).thenReturn(disciplina);
        when(questao.getAssunto()).thenReturn(assunto);
        when(resolvedor.resolver(eq(usuarioId), any())).thenReturn(new ContextoOrigem(questao, null));
        var resultado = service.criar(usuarioId, requisicao("Frente", "Verso", null, UUID.randomUUID(), UUID.randomUUID()));
        var salvo = resultado.flashcard();
        assertThat(salvo.disciplina()).isNotNull();
        var card = capturarCard();
        assertThat(card.getOrigem().getDisciplina()).isSameAs(disciplina);
        assertThat(card.getOrigem().getAssunto()).isSameAs(assunto);
    }

    @Test
    void TU09_recusaFrenteComDoisCaracteresSemPersistir() {
        var erro = assertThatThrownBy(() -> service.criar(usuarioId, requisicao("ab", "verso", null, null, null)))
                .isInstanceOf(DadosInvalidosException.class).actual();
        assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("frente");
        verifyNoInteractions(flashcards, resolvedor);
    }

    @Test
    void TU10_recusaVersoSomenteEspacosSemPersistir() {
        var erro = assertThatThrownBy(() -> service.criar(usuarioId, requisicao("frente", "   ", null, null, null)))
                .isInstanceOf(DadosInvalidosException.class).actual();
        assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("verso");
        verifyNoInteractions(flashcards, resolvedor);
    }

    @Test
    void TU11_criaCardAvulsoComDadosDoCorpoEOrigemNula() {
        var disciplinaId = UUID.randomUUID();
        var disciplina = mock(Disciplina.class);
        when(entityManager.getReference(Disciplina.class, disciplinaId)).thenReturn(disciplina);
        when(resolvedor.resolver(eq(usuarioId), any())).thenReturn(ContextoOrigem.vazio());
        var resultado = service.criar(usuarioId, requisicao("Frente", "Verso", null, disciplinaId, null));
        assertThat(resultado.flashcard().origem()).isNull();
        assertThat(capturarCard().getOrigem().getDisciplina()).isSameAs(disciplina);
    }

    @Test
    void TU12_criaCardDevolvendoTotalDaSessao() {
        var sessaoId = UUID.randomUUID();
        var sessao = mock(SessaoEstudo.class);
        when(sessao.getId()).thenReturn(sessaoId);
        when(resolvedor.resolver(eq(usuarioId), any())).thenReturn(new ContextoOrigem(mock(Questao.class), sessao));
        when(flashcards.countByUsuarioIdAndOrigemSessaoOrigemId(usuarioId, sessaoId)).thenReturn(4L);
        assertThat(service.criar(usuarioId, requisicao("Frente", "Verso", null, null, null)).criadosNaSessao()).isEqualTo(4L);
        verify(flashcards).countByUsuarioIdAndOrigemSessaoOrigemId(usuarioId, sessaoId);
    }

    @Test
    void TU13_criaCardAvulsoSemContarSessao() {
        when(resolvedor.resolver(eq(usuarioId), any())).thenReturn(ContextoOrigem.vazio());
        assertThat(service.criar(usuarioId, requisicao("Frente", "Verso", null, null, null)).criadosNaSessao()).isNull();
        verify(flashcards, never()).countByUsuarioIdAndOrigemSessaoOrigemId(any(), any());
    }

    @Test
    void TU14_atualizaFrenteVersoDeckETags() {
        var card = cardAtivo();
        var deck = mock(Deck.class);
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        when(decks.findByIdAndUsuarioId(any(), eq(usuarioId))).thenReturn(Optional.of(deck));
        service.atualizar(UUID.randomUUID(), usuarioId, requisicao(" Nova frente ", " Novo verso ", UUID.randomUUID(), null, null));
        assertThat(card.getFrente()).isEqualTo("Nova frente");
        assertThat(card.getVerso()).isEqualTo("Novo verso");
        assertThat(card.getDeck()).isSameAs(deck);
        assertThat(card.getTags()).containsExactly("direito");
    }

    @Test
    void TU15_atualizaIgnorandoTrocaDeOrigem() {
        var card = cardAtivo();
        card.getOrigem().setQuestaoOrigem(mock(Questao.class));
        var origem = card.getOrigem().getQuestaoOrigem();
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        service.atualizar(UUID.randomUUID(), usuarioId, requisicao("Frente", "Verso", null, null, null));
        assertThat(card.getOrigem().getQuestaoOrigem()).isSameAs(origem);
    }

    private Flashcard cardAtivo() { return new Flashcard(mock(Usuario.class), "frente", "verso"); }
    private Flashcard capturarCard() { var captor = org.mockito.ArgumentCaptor.forClass(Flashcard.class); verify(flashcards).save(captor.capture()); return captor.getValue(); }
    private RequisicaoFlashcard requisicao(String frente, String verso, UUID deck, UUID disciplina, UUID assunto) {
        return new RequisicaoFlashcard(frente, verso, deck, disciplina, assunto, List.of(" Direito "), null, null);
    }
}
