package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import br.com.certame.questao.Questao;
import br.com.certame.estudo.SessaoEstudo;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolvedorContextoFlashcardTest {
    private final QuestaoPublicada questoes = mock(QuestaoPublicada.class);
    private final SessaoDoUsuario sessoes = mock(SessaoDoUsuario.class);
    private final ResolvedorContextoFlashcard resolvedor = new ResolvedorContextoFlashcard(questoes, sessoes);

    @Test
    void TU21_recusaQuestaoNaoPublicada() {
        var id = UUID.randomUUID();
        when(questoes.buscarPublicada(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolvedor.resolver(UUID.randomUUID(), requisicao(id, null)))
                .isInstanceOf(DadosInvalidosException.class)
                .satisfies(erro -> assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("questaoOrigemId"));
    }

    @Test
    void TU22_recusaSessaoDeOutroUsuario() {
        var questaoId = UUID.randomUUID();
        var sessaoId = UUID.randomUUID();
        when(questoes.buscarPublicada(questaoId)).thenReturn(Optional.of(mock(Questao.class)));
        when(sessoes.buscar(sessaoId, UUID.randomUUID())).thenReturn(Optional.empty());
        var usuario = UUID.randomUUID();
        when(sessoes.buscar(eq(sessaoId), eq(usuario))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolvedor.resolver(usuario, requisicao(questaoId, sessaoId)))
                .isInstanceOf(DadosInvalidosException.class)
                .satisfies(erro -> assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("sessaoOrigemId"));
    }

    @Test
    void TU23_recusaSessaoSemQuestao() {
        var sessaoId = UUID.randomUUID();
        assertThatThrownBy(() -> resolvedor.resolver(UUID.randomUUID(), requisicao(null, sessaoId)))
                .isInstanceOf(DadosInvalidosException.class)
                .satisfies(erro -> assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsEntry(
                        "sessaoOrigemId", "Sessão de estudo não encontrada."));
        verifyNoInteractions(questoes, sessoes);
    }

    @Test
    void TU24_devolveContextoVazioSemConsultarOrigens() {
        var contexto = resolvedor.resolver(UUID.randomUUID(), requisicao(null, null));
        assertThat(contexto).isEqualTo(ContextoOrigem.vazio());
        verifyNoInteractions(questoes, sessoes);
    }

    private RequisicaoFlashcard requisicao(UUID questao, UUID sessao) {
        return new RequisicaoFlashcard("frente", "verso", null, null, null, null, questao, sessao);
    }
}
