package br.com.certame.comum;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaRespostaTest {
    @Test void TU01_preservaEnvelopeDaPagina() {
        var resposta = new PaginaResposta<>(List.of("item"), 1, 20, 21);

        assertThat(resposta.content()).containsExactly("item");
        assertThat(resposta.page()).isEqualTo(1);
        assertThat(resposta.size()).isEqualTo(20);
        assertThat(resposta.totalElements()).isEqualTo(21);
    }
}
