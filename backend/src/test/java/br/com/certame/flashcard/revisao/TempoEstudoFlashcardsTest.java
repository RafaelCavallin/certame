package br.com.certame.flashcard.revisao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TempoEstudoFlashcardsTest {
    private static final LocalDate DIA = LocalDate.of(2026, 3, 4);
    private static final Instant INICIO = Instant.parse("2026-03-04T03:00:00Z");
    private static final Instant FIM = Instant.parse("2026-03-05T03:00:00Z");
    @Mock SessaoRevisaoRepository sessoes;
    private final UUID usuarioId = UUID.randomUUID();
    private TempoEstudoFlashcards porta;

    @BeforeEach
    void preparar() {
        porta = new TempoEstudoFlashcards(sessoes);
    }

    @Test
    void TU54_somaSoSessoesFinalizadasDoDia() {
        when(sessoes.somarTempoMsFinalizadasEntre(usuarioId, INICIO, FIM)).thenReturn(720_000L);

        var segundos = porta.segundosNoDia(usuarioId, DIA);

        assertThat(segundos).isEqualTo(720);
    }

    @Test
    void TU54b_devolveZeroSemSessoesFinalizadasNoDia() {
        when(sessoes.somarTempoMsFinalizadasEntre(usuarioId, INICIO, FIM)).thenReturn(0L);

        var segundos = porta.segundosNoDia(usuarioId, DIA);

        assertThat(segundos).isZero();
    }
}
