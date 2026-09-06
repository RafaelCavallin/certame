package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.dto.FiltroQuestao;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuscaQuestaoServiceTest {
    private final QuestaoRepository repositorio = mock(QuestaoRepository.class);
    private final BuscaQuestaoService service = new BuscaQuestaoService(repositorio, new SimpleMeterRegistry());

    @Test
    void reconheceCodigoComOuSemHifenComoPrefixo() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(new PaginaResposta<>(List.of(), 0, 20, 0));
        service.buscar(new FiltroQuestao("Q-88409", null, null, null, null, null, null), new Paginacao(0, 20));
        verify(repositorio).buscar(any(), eq(new Paginacao(0, 20)), eq("Q-88409"));
        reset(repositorio);
        when(repositorio.buscar(any(), any(), any())).thenReturn(new PaginaResposta<>(List.of(), 0, 20, 0));
        service.buscar(new FiltroQuestao("88409", null, null, null, null, null, null), new Paginacao(0, 20));
        verify(repositorio).buscar(any(), any(), eq("Q-88409"));
    }

    @Test
    void combinaTermoEFiltrosNoFiltroEnviadoAoRepositorio() {
        var disciplina = UUID.randomUUID();
        var banca = UUID.randomUUID();
        when(repositorio.buscar(any(), any(), any())).thenReturn(new PaginaResposta<>(List.of(), 0, 20, 0));
        service.buscar(new FiltroQuestao("licitações", disciplina, null, banca, null, TipoQuestao.ME, StatusQuestao.RASCUNHO), new Paginacao(0, 20));
        var filtro = org.mockito.ArgumentCaptor.forClass(FiltroQuestao.class);
        verify(repositorio).buscar(filtro.capture(), any(), isNull());
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().disciplina()).isEqualTo(disciplina);
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().banca()).isEqualTo(banca);
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().tipo()).isEqualTo(TipoQuestao.ME);
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().status()).isEqualTo(StatusQuestao.RASCUNHO);
    }

    @Test
    void buscarPublicaRepassaOFiltroJaMontadoPeloControlador() {
        when(repositorio.buscar(any(), any(), any())).thenReturn(new PaginaResposta<>(List.of(), 0, 20, 0));
        service.buscarPublica(new FiltroQuestao(null, null, null, null, null, null, StatusQuestao.PUBLICADA), new Paginacao(0, 20));
        var filtro = org.mockito.ArgumentCaptor.forClass(FiltroQuestao.class);
        verify(repositorio).buscar(filtro.capture(), any(), isNull());
        org.assertj.core.api.Assertions.assertThat(filtro.getValue().status()).isEqualTo(StatusQuestao.PUBLICADA);
    }

    @Test
    void rejeitaOrdenacaoForaDaListaBranca() {
        assertThatThrownBy(() -> service.buscar(new FiltroQuestao(null, null, null, null, null, null, null), new Paginacao(0, 20, "enunciado,desc")))
                .isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(repositorio);
    }
}
