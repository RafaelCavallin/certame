package br.com.certame.estudo.caderno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferenciaCadernoTest {
    @Test void TU18_interpretaReferenciasAutomaticasEManual() {
        var favorita = ReferenciaCaderno.de("auto:FAVORITA");
        var erro = ReferenciaCaderno.de("auto:ERRO_RECORRENTE");
        var id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        var manual = ReferenciaCaderno.de(id.toString());
        assertThat(favorita.automatico()).isTrue();
        assertThat(favorita.tipoAutomatico()).isEqualTo(TipoCadernoAutomatico.FAVORITA);
        assertThat(erro.tipoAutomatico()).isEqualTo(TipoCadernoAutomatico.ERRO_RECORRENTE);
        assertThat(manual.automatico()).isFalse();
        assertThat(manual.manualId()).isEqualTo(id);
    }

    @Test void TU19_recusaReferenciaInvalida() {
        assertThatThrownBy(() -> ReferenciaCaderno.de("auto:DESCONHECIDO"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(excecao -> ((RecursoNaoEncontradoException) excecao).getCode())
                .isEqualTo("CADERNO_NAO_ENCONTRADO");
        assertThatThrownBy(() -> ReferenciaCaderno.de("nao-e-uuid"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(excecao -> ((RecursoNaoEncontradoException) excecao).getCode())
                .isEqualTo("CADERNO_NAO_ENCONTRADO");
    }

    @Test void TU20_recusaReferenciaNulaOuVaziaComCodigoCadernoNaoEncontrado() {
        assertThatThrownBy(() -> ReferenciaCaderno.de(null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(excecao -> ((RecursoNaoEncontradoException) excecao).getCode())
                .isEqualTo("CADERNO_NAO_ENCONTRADO");
        assertThatThrownBy(() -> ReferenciaCaderno.de(""))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(excecao -> ((RecursoNaoEncontradoException) excecao).getCode())
                .isEqualTo("CADERNO_NAO_ENCONTRADO");
    }
}
