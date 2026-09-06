package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.com.certame.comum.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

class TamanhoSerieTest {
    @Test void TU06_aceitaTamanhosPermitidosERecusaOsDemais() {
        assertThat(new TamanhoSerie(10).valor()).isEqualTo(10);
        assertThat(new TamanhoSerie(30).valor()).isEqualTo(30);
        assertThat(new TamanhoSerie(50).valor()).isEqualTo(50);
        assertThatThrownBy(() -> new TamanhoSerie(7)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new TamanhoSerie(51)).isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> new TamanhoSerie(-1)).isInstanceOf(DadosInvalidosException.class);
    }
}
