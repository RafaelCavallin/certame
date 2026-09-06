package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificadorDonoFlashcardTest {
    @Test
    void TU01_devolveCardDoProprioUsuario() {
        var id = UUID.randomUUID();
        var usuario = UUID.randomUUID();
        var card = mock(Flashcard.class);
        var repo = mock(FlashcardRepository.class);
        when(repo.findByIdAndUsuarioIdAndStatusNot(id, usuario, StatusFlashcard.EXCLUIDO)).thenReturn(Optional.of(card));
        var resultado = new VerificadorDonoFlashcard(repo).exigirProprio(id, usuario);
        assertThat(resultado).isSameAs(card);
        verify(repo).findByIdAndUsuarioIdAndStatusNot(id, usuario, StatusFlashcard.EXCLUIDO);
    }

    @Test
    void TU02_recusaCardDeOutroUsuarioComoNaoEncontrado() {
        var repo = mock(FlashcardRepository.class);
        when(repo.findByIdAndUsuarioIdAndStatusNot(any(), any(), eq(StatusFlashcard.EXCLUIDO))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new VerificadorDonoFlashcard(repo).exigirProprio(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessage("O recurso solicitado não foi encontrado.");
    }

    @Test
    void TU03_recusaCardExcluidoComAMesmaMensagemDoInexistente() {
        var repo = mock(FlashcardRepository.class);
        when(repo.findByIdAndUsuarioIdAndStatusNot(any(), any(), eq(StatusFlashcard.EXCLUIDO))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new VerificadorDonoFlashcard(repo).exigirProprio(UUID.randomUUID(), UUID.randomUUID()))
                .hasMessage("O recurso solicitado não foi encontrado.");
    }
}
