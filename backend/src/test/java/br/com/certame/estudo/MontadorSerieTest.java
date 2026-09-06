package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import br.com.certame.comum.excecao.SerieVaziaException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MontadorSerieTest {
    private static final FiltroSerie FILTRO = new FiltroSerie(null, null, null, null, null, false, false);
    private static final UUID USUARIO = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test void TU07_montaSerieComQuestoesElegiveis() {
        var ids = ids(3);
        var repositorio = mock(SerieRepository.class);
        when(repositorio.buscarElegiveis(FILTRO, USUARIO)).thenReturn(ids);
        var serie = new MontadorSerie(repositorio, new Random(7)).montar(FILTRO, new TamanhoSerie(10), USUARIO);
        assertThat(serie).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test void TU08_naoRepeteQuestaoDentroDaSerie() {
        var ids = ids(30);
        var repositorio = mock(SerieRepository.class);
        when(repositorio.buscarElegiveis(FILTRO, USUARIO)).thenReturn(ids);
        var serie = new MontadorSerie(repositorio, new Random(7)).montar(FILTRO, new TamanhoSerie(30), USUARIO);
        assertThat(serie).doesNotHaveDuplicates().hasSize(30);
    }

    @Test void TU09_devolveSerieMenorQuandoAcervoNaoBasta() {
        var repositorio = mock(SerieRepository.class);
        when(repositorio.buscarElegiveis(FILTRO, USUARIO)).thenReturn(ids(12));
        var serie = new MontadorSerie(repositorio, new Random(7)).montar(FILTRO, new TamanhoSerie(30), USUARIO);
        assertThat(serie).hasSize(12);
    }

    @Test void TU10_lancaErroQuandoNaoHaQuestaoElegivel() {
        var repositorio = mock(SerieRepository.class);
        when(repositorio.buscarElegiveis(FILTRO, USUARIO)).thenReturn(List.of());
        assertThatThrownBy(() -> new MontadorSerie(repositorio, new Random(7))
                .montar(FILTRO, new TamanhoSerie(10), USUARIO))
                .isInstanceOf(SerieVaziaException.class)
                .hasMessage("Nenhuma questão publicada atende a esses filtros. Amplie a busca e tente de novo.");
    }

    private List<UUID> ids(int quantidade) {
        return java.util.stream.IntStream.range(0, quantidade)
                .mapToObj(indice -> UUID.nameUUIDFromBytes(("questao-" + indice).getBytes()))
                .toList();
    }
}
