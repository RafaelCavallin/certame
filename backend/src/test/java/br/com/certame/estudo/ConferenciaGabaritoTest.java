package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.TipoQuestao;
import org.junit.jupiter.api.Test;

class ConferenciaGabaritoTest {
    private final ConferenciaGabarito conferencia = new ConferenciaGabarito();

    @Test void TU01_acertaMultiplaEscolhaComLetraCorreta() {
        assertThat(conferencia.corrigir(TipoQuestao.ME, "B", "B")).isTrue();
    }

    @Test void TU02_erraMultiplaEscolhaComLetraDiferente() {
        assertThat(conferencia.corrigir(TipoQuestao.ME, "B", "D")).isFalse();
    }

    @Test void TU03_normalizaCaixaDaMarcacao() {
        assertThat(conferencia.corrigir(TipoQuestao.ME, "B", " b ")).isTrue();
    }

    @Test void TU04_recusaMarcacaoForaDoDominioDeMultiplaEscolha() {
        assertThat(conferencia.valida(TipoQuestao.ME, "F")).isFalse();
        assertThatThrownBy(() -> conferencia.corrigir(TipoQuestao.ME, "A", "F"))
                .isInstanceOfSatisfying(DadosInvalidosException.class, excecao -> {
                    assertThat(excecao).hasMessage("Verifique os campos destacados.");
                    assertThat(excecao.getFieldErrors()).containsKey("marcada");
                });
    }

    @Test void TU05_aceitaCertoEErradoEmCertoErrado() {
        assertThat(conferencia.valida(TipoQuestao.CE, "CERTO")).isTrue();
        assertThat(conferencia.valida(TipoQuestao.CE, "ERRADO")).isTrue();
        assertThat(conferencia.valida(TipoQuestao.CE, "A")).isFalse();
    }
}
