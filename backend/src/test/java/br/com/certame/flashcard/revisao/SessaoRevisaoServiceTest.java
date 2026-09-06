package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.deck.DeckRepository;
import br.com.certame.flashcard.revisao.dto.FaixaVencimento;
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
class SessaoRevisaoServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T13:02:11Z");
    @Mock SessaoRevisaoRepository sessoes;
    @Mock FlashcardRevisaoRepository registros;
    @Mock DeckRepository decks;
    @Mock MontadorFila montador;
    @Mock ConstrutorCardRevisao construtorCard;
    @Mock ResumoRevisaoService resumo;
    @Mock RelogioEstudo relogio;
    @Mock EntityManager entityManager;
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sessaoId = UUID.randomUUID();
    private SessaoRevisaoService service;
    private SessaoRevisao sessao;

    @BeforeEach
    void preparar() {
        service = new SessaoRevisaoService(sessoes, registros, decks, montador, construtorCard, resumo, relogio,
                entityManager, new LocalizadorSessaoRevisao(sessoes), Clock.fixed(AGORA, ZoneOffset.UTC));
        sessao = new SessaoRevisao(mock(Usuario.class), "TODOS", null, null);
        lenient().when(sessoes.findByIdAndUsuarioId(sessaoId, usuarioId)).thenReturn(Optional.of(sessao));
    }

    @Test
    void TU46_finalizarConsolidaContagensETempo() {
        var cardA = mock(Flashcard.class);
        when(cardA.getId()).thenReturn(UUID.randomUUID());
        var cardB = mock(Flashcard.class);
        when(cardB.getId()).thenReturn(UUID.randomUUID());
        var registrosDaSessao = List.of(
                logDe(cardA, "BOM", 1000),
                logDe(cardA, "ERREI", 2000),
                logDe(cardB, "FACIL", 3000));
        when(registros.findBySessaoIdOrderByRevisadoEmAsc(sessaoId)).thenReturn(registrosDaSessao);
        var faixas = List.of(new FaixaVencimento(FaixaVencimento.AMANHA, "Amanhã", 0));
        when(resumo.proximosVencimentos(usuarioId)).thenReturn(faixas);

        var resumoSessao = service.finalizar(sessaoId, usuarioId);

        assertThat(resumoSessao.revisados()).isEqualTo(2);
        assertThat(resumoSessao.classificacoes()).isEqualTo(3);
        assertThat(resumoSessao.distribuicao().errei()).isOne();
        assertThat(resumoSessao.distribuicao().bom()).isOne();
        assertThat(resumoSessao.distribuicao().facil()).isOne();
        assertThat(resumoSessao.distribuicao().dificil()).isZero();
        assertThat(resumoSessao.tempoMs()).isEqualTo(6000);
        assertThat(resumoSessao.proximosVencimentos()).isEqualTo(faixas);
        assertThat(sessao.getFinalizadaEm()).isEqualTo(AGORA);
        assertThat(sessao.getRevisados()).isEqualTo((short) 2);
    }

    @Test
    void TU47_finalizarRecusaSegundaFinalizacao() {
        sessao.finalizar(AGORA.minusSeconds(60), (short) 5, (short) 1, (short) 1, (short) 2, (short) 1, 9000);

        var erro = assertThatThrownBy(() -> service.finalizar(sessaoId, usuarioId))
                .isInstanceOf(ConflitoException.class).actual();

        assertThat(((ConflitoException) erro).getCode()).isEqualTo("SESSAO_FINALIZADA");
        assertThat(sessao.getRevisados()).isEqualTo((short) 5);
        assertThat(sessao.getTempoMs()).isEqualTo(9000);
    }

    private FlashcardRevisao logDe(Flashcard card, String nota, int tempoMs) {
        return new FlashcardRevisao(card, card.getUsuario(), sessao, nota, (short) 0, 0f, 1f, 0f, 1f, (short) 1,
                1440, tempoMs, tempoMs / 1000);
    }
}
