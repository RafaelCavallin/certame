package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ConsultaFlashcardsServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T14:02:11Z");
    private static final LocalDate HOJE = LocalDate.parse("2026-08-30");
    @Mock FlashcardRepository flashcards;
    @Mock VerificadorDonoFlashcard verificador;
    private final UUID usuarioId = UUID.randomUUID();
    private ConsultaFlashcardsService service;

    @BeforeEach
    void preparar() {
        service = new ConsultaFlashcardsService(flashcards, verificador, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void TU25_calculaOsIndicadoresGlobaisSobreCardsAtivos() {
        when(flashcards.buscar(eq(usuarioId), any(), eq(StatusFlashcard.ATIVO), any())).thenReturn(new PageImpl<>(List.of()));
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(usuarioId, StatusFlashcard.ATIVO, HOJE)).thenReturn(12L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(usuarioId, StatusFlashcard.ATIVO, HOJE.plusDays(7))).thenReturn(34L);
        when(flashcards.countByUsuarioIdAndStatusAndAgendamentoEstadoIn(eq(usuarioId), eq(StatusFlashcard.ATIVO), any())).thenReturn(9L);
        when(flashcards.countByUsuarioIdAndStatus(usuarioId, StatusFlashcard.ATIVO)).thenReturn(128L);
        when(flashcards.contarDecksComCardAtivo(usuarioId, StatusFlashcard.ATIVO)).thenReturn(4L);

        var pagina = service.listar(usuarioId, filtroVazio(), Pageable.ofSize(20));

        assertThat(pagina.indicadores().vencendoHoje()).isEqualTo(12);
        assertThat(pagina.indicadores().proximosSeteDias()).isEqualTo(34);
        assertThat(pagina.indicadores().emFixacao()).isEqualTo(9);
        assertThat(pagina.indicadores().total()).isEqualTo(128);
        assertThat(pagina.indicadores().decks()).isEqualTo(4);
    }

    @Test
    void TU26_indicadoresNaoMudamSobFiltroSoContentETotalMudam() {
        stubIndicadores();
        when(flashcards.buscar(eq(usuarioId), any(), eq(StatusFlashcard.ATIVO), any()))
                .thenReturn(new PageImpl<>(List.of()))
                .thenReturn(new PageImpl<>(List.of(cardAtivo())));

        var semFiltro = service.listar(usuarioId, filtroVazio(), Pageable.ofSize(20));
        var comFiltro = service.listar(usuarioId,
                new FiltroFlashcards("controle concentrado", null, null, EstadoFlashcard.NOVO, false), Pageable.ofSize(20));

        assertThat(comFiltro.indicadores()).isEqualTo(semFiltro.indicadores());
        assertThat(semFiltro.content()).isEmpty();
        assertThat(semFiltro.totalElements()).isZero();
        assertThat(comFiltro.content()).hasSize(1);
    }

    @Test
    void TU27_recusaOrdenacaoForaDaListaBrancaAntesDoRepositorio() {
        var pageable = PageRequest.of(0, 20, Sort.by("frente").ascending());

        assertThatThrownBy(() -> service.listar(usuarioId, filtroVazio(), pageable))
                .isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(flashcards);
    }

    private void stubIndicadores() {
        lenient().when(flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(any(), any(), any())).thenReturn(1L);
        lenient().when(flashcards.countByUsuarioIdAndStatusAndAgendamentoEstadoIn(any(), any(), any())).thenReturn(1L);
        lenient().when(flashcards.countByUsuarioIdAndStatus(any(), any())).thenReturn(1L);
        lenient().when(flashcards.contarDecksComCardAtivo(any(), any())).thenReturn(1L);
    }

    private FiltroFlashcards filtroVazio() { return new FiltroFlashcards(null, null, null, null, false); }
    private Flashcard cardAtivo() { return new Flashcard(mock(Usuario.class), "frente", "verso"); }
}
