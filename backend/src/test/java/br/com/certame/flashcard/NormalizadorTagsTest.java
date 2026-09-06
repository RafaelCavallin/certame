package br.com.certame.flashcard;

import static org.assertj.core.api.Assertions.*;

import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NormalizadorTagsTest {
    @Test
    void TU04_normalizaMaiusculasEspacosEDuplicatas() {
        assertThat(new NormalizadorTags().normalizar(java.util.List.of(" ADI ", "adi", "STF")))
                .containsExactly("adi", "stf");
    }

    @Test
    void TU05_recusaMaisDeDezTags() {
        var tags = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();
        assertThatThrownBy(() -> new NormalizadorTags().normalizar(tags)).isInstanceOf(DadosInvalidosException.class)
                .satisfies(erro -> assertThat(((DadosInvalidosException) erro).getFieldErrors()).containsKey("tags"));
    }

    @Test
    void TU06_transformaNuloVaziosESpacosEmListaVazia() {
        assertThat(new NormalizadorTags().normalizar(Arrays.asList(null, "", "  "))).isEmpty();
        assertThat(new NormalizadorTags().normalizar(null)).isEmpty();
    }
}
