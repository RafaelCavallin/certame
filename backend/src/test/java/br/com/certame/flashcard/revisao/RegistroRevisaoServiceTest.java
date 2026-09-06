package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.NotaRevisao;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.StatusFlashcard;
import br.com.certame.flashcard.fsrs.AgendadorFsrs;
import br.com.certame.flashcard.fsrs.ContextoAgendamento;
import br.com.certame.flashcard.fsrs.EstadoMemoria;
import br.com.certame.flashcard.revisao.dto.CardRevisao;
import br.com.certame.flashcard.revisao.dto.RequisicaoRevisao;
import br.com.certame.usuario.Usuario;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistroRevisaoServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T13:02:11Z");
    @Mock SessaoRevisaoRepository sessoes;
    @Mock FlashcardRepository flashcards;
    @Mock FlashcardRevisaoRepository logs;
    @Mock ConstrutorCardRevisao construtorCard;
    @Mock RelogioEstudo relogio;
    private final AgendadorFsrs agendadorReal = new AgendadorFsrs();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sessaoId = UUID.randomUUID();
    private RegistroRevisaoService service;
    private SessaoRevisao sessao;

    @BeforeEach
    void preparar() {
        service = new RegistroRevisaoService(flashcards, logs, construtorCard,
                new LocalizadorSessaoRevisao(sessoes), relogio);
        sessao = new SessaoRevisao(mock(Usuario.class), "TODOS", null, null);
        lenient().when(sessoes.findByIdAndUsuarioId(sessaoId, usuarioId)).thenReturn(Optional.of(sessao));
        lenient().when(relogio.instante()).thenReturn(AGORA);
    }

    @Test
    void TU40_gravaOLogCompletoDaRevisao() {
        var card = cardPrimeiroContato();
        when(flashcards.findByIdAndUsuarioIdAndStatus(card.getId(), usuarioId, StatusFlashcard.ATIVO))
                .thenReturn(Optional.of(card));

        service.registrar(sessaoId, usuarioId, requisicao(card.getId(), NotaRevisao.BOM, 4200));

        var esperado = agendadorReal.proximo(new EstadoMemoria(0, 0, EstadoFlashcard.NOVO, 0), NotaRevisao.BOM,
                new ContextoAgendamento(0.9, 180, 0, AGORA));
        var log = capturarLog();
        assertThat(log.getNota()).isEqualTo("BOM");
        assertThat(log.getDiasDecorridos()).isZero();
        assertThat(log.getEstabilidadeAnt()).isZero();
        assertThat(log.getEstabilidadeNova()).isCloseTo((float) esperado.estabilidade(), offset(1e-3f));
        assertThat(log.getDificuldadeNova()).isCloseTo((float) esperado.dificuldade(), offset(1e-3f));
        assertThat(log.getIntervaloNovo()).isEqualTo((short) esperado.intervaloDias());
        assertThat(log.getIntervaloMinutos()).isEqualTo(esperado.intervaloMinutos());
        assertThat(log.getTempoMs()).isEqualTo(4200);
        assertThat(log.getSegundosDecorridos()).isEqualTo(4);
        assertThat(log.getSessao()).isSameAs(sessao);
        assertThat(log.getFlashcard()).isSameAs(card);
    }

    @Test
    void TU41_atualizaAMemoriaEAsDuasColunasDeAgenda() {
        var card = cardPrimeiroContato();
        when(flashcards.findByIdAndUsuarioIdAndStatus(card.getId(), usuarioId, StatusFlashcard.ATIVO))
                .thenReturn(Optional.of(card));

        var resposta = service.registrar(sessaoId, usuarioId, requisicao(card.getId(), NotaRevisao.BOM, 1000));

        assertThat(card.getAgendamento().getEstabilidade()).isCloseTo((float) resposta.estabilidade(), offset(1e-2f));
        assertThat(card.getAgendamento().getEstado()).isEqualTo(EstadoFlashcard.APRENDENDO);
        assertThat(card.getAgendamento().getProximaRevisaoEm()).isEqualTo(resposta.proximaRevisaoEm());
        assertThat(card.getAgendamento().getUltimaRevisao()).isEqualTo(AGORA);
    }

    @Test
    void TU42_marcaReenfileiramentoEmPassoIntradiario() {
        var card = cardEmRevisao();
        when(flashcards.findByIdAndUsuarioIdAndStatus(card.getId(), usuarioId, StatusFlashcard.ATIVO))
                .thenReturn(Optional.of(card));
        var cardDto = mock(CardRevisao.class);
        when(construtorCard.de(card)).thenReturn(cardDto);

        var resposta = service.registrar(sessaoId, usuarioId, requisicao(card.getId(), NotaRevisao.ERREI, 2000));

        assertThat(resposta.reenfileirar()).isTrue();
        assertThat(resposta.card()).isSameAs(cardDto);
        assertThat(card.getAgendamento().getEstado()).isEqualTo(EstadoFlashcard.REAPRENDENDO);
        assertThat(resposta.intervaloMinutos()).isEqualTo(10);
    }

    @Test
    void TU43_truncaTempoMsAcimaDoMaximo() {
        var card = cardPrimeiroContato();
        when(flashcards.findByIdAndUsuarioIdAndStatus(card.getId(), usuarioId, StatusFlashcard.ATIVO))
                .thenReturn(Optional.of(card));

        service.registrar(sessaoId, usuarioId, requisicao(card.getId(), NotaRevisao.BOM, 900_000));

        var log = capturarLog();
        assertThat(log.getTempoMs()).isEqualTo(600_000);
        assertThat(log.getSegundosDecorridos()).isEqualTo(600);
    }

    @Test
    void TU44_recusaCardDeOutroUsuarioSemGravarLog() {
        var flashcardId = UUID.randomUUID();
        when(flashcards.findByIdAndUsuarioIdAndStatus(flashcardId, usuarioId, StatusFlashcard.ATIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(sessaoId, usuarioId, requisicao(flashcardId, NotaRevisao.BOM, 1000)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(logs);
    }

    @Test
    void TU45_recusaRevisaoEmSessaoFinalizada() {
        sessao.finalizar(AGORA, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, 0);

        var erro = assertThatThrownBy(
                () -> service.registrar(sessaoId, usuarioId, requisicao(UUID.randomUUID(), NotaRevisao.BOM, 1000)))
                .isInstanceOf(ConflitoException.class).actual();

        assertThat(((ConflitoException) erro).getCode()).isEqualTo("SESSAO_FINALIZADA");
        verifyNoInteractions(flashcards, logs);
    }

    private FlashcardRevisao capturarLog() {
        var captor = ArgumentCaptor.forClass(FlashcardRevisao.class);
        verify(logs).save(captor.capture());
        return captor.getValue();
    }

    private Flashcard cardPrimeiroContato() {
        return new Flashcard(mock(Usuario.class), "frente", "verso");
    }

    private Flashcard cardEmRevisao() {
        var card = new Flashcard(mock(Usuario.class), "frente", "verso");
        card.getAgendamento().setEstabilidade(10f);
        card.getAgendamento().setDificuldade(6f);
        card.getAgendamento().setEstado(EstadoFlashcard.REVISAO);
        card.getAgendamento().setUltimaRevisao(AGORA.minusSeconds(864_000));
        return card;
    }

    private RequisicaoRevisao requisicao(UUID flashcardId, NotaRevisao nota, int tempoMs) {
        return new RequisicaoRevisao(flashcardId, nota, tempoMs);
    }
}
