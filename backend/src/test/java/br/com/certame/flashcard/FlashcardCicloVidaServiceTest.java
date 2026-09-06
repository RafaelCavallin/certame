package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlashcardCicloVidaServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T14:02:11Z");
    @Mock VerificadorDonoFlashcard verificador;
    private final UUID usuarioId = UUID.randomUUID();
    private FlashcardCicloVidaService service;

    @BeforeEach
    void preparar() {
        service = new FlashcardCicloVidaService(verificador, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Flashcard cardAtivo() { return new Flashcard(mock(Usuario.class), "frente", "verso"); }

    @Test
    void TU16_arquivaCardAtivoComInstanteDoClock() {
        var card = cardAtivo();
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        service.arquivar(UUID.randomUUID(), usuarioId);
        assertThat(card.getStatus()).isEqualTo(StatusFlashcard.ARQUIVADO);
        assertThat(card.getRegistroTemporal().getArquivadoEm()).isEqualTo(AGORA);
    }

    @Test
    void TU17_recusaArquivarCardJaArquivadoSemEfeito() {
        var card = cardAtivo();
        card.setStatus(StatusFlashcard.ARQUIVADO);
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        assertThatThrownBy(() -> service.arquivar(UUID.randomUUID(), usuarioId)).isInstanceOf(ConflitoException.class);
        assertThat(card.getRegistroTemporal().getArquivadoEm()).isNull();
    }

    @Test
    void TU18_restauraCardArquivado() {
        var card = cardAtivo();
        card.setStatus(StatusFlashcard.ARQUIVADO);
        card.getRegistroTemporal().setArquivadoEm(AGORA);
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        service.restaurar(UUID.randomUUID(), usuarioId);
        assertThat(card.getStatus()).isEqualTo(StatusFlashcard.ATIVO);
        assertThat(card.getRegistroTemporal().getArquivadoEm()).isNull();
    }

    @Test
    void TU19_excluiCardComHistoricoSemDelete() {
        var card = cardAtivo();
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenReturn(card);
        service.excluir(UUID.randomUUID(), usuarioId);
        assertThat(card.getStatus()).isEqualTo(StatusFlashcard.EXCLUIDO);
        assertThat(card.getRegistroTemporal().getExcluidoEm()).isEqualTo(AGORA);
    }

    @Test
    void TU20_recusaCardDeOutroUsuarioSemEscrita() {
        when(verificador.exigirProprio(any(), eq(usuarioId))).thenThrow(new RecursoNaoEncontradoException());
        assertThatThrownBy(() -> service.excluir(UUID.randomUUID(), usuarioId)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
