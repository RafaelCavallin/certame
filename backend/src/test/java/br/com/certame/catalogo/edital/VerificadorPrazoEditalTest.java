package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificadorPrazoEditalTest {
    private static final Clock HOJE_FIXO = Clock.fixed(
            LocalDate.of(2026, 9, 1).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private final VerificadorPrazoEdital verificador = new VerificadorPrazoEdital(HOJE_FIXO);

    @Test void TU01_sinalizaEditalAbertoComPrazoDeOntem() {
        // Arrange
        var prazo = LocalDate.of(2026, 8, 31);

        // Act
        var vencido = verificador.vencido(StatusEdital.ABERTO, prazo);

        // Assert
        assertThat(vencido).isTrue();
    }

    @Test void TU02_naoSinalizaEditalAbertoComPrazoDeHoje() {
        // Arrange
        var prazo = LocalDate.of(2026, 9, 1);

        // Act
        var vencido = verificador.vencido(StatusEdital.ABERTO, prazo);

        // Assert
        assertThat(vencido).isFalse();
    }

    @Test void TU03_naoSinalizaEditalAbertoComPrazoFuturo() {
        // Arrange
        var prazo = LocalDate.of(2026, 9, 2);

        // Act
        var vencido = verificador.vencido(StatusEdital.ABERTO, prazo);

        // Assert
        assertThat(vencido).isFalse();
    }

    @Test void TU04_naoSinalizaEditalEncerradoComPrazoPassado() {
        // Arrange
        var prazo = LocalDate.of(2026, 8, 31);

        // Act
        var vencido = verificador.vencido(StatusEdital.ENCERRADO, prazo);

        // Assert
        assertThat(vencido).isFalse();
    }

    @Test void TU04b_naoLancaNPEQuandoPrazoNuloParaAberto() {
        // Act
        var vencido = verificador.vencido(StatusEdital.ABERTO, null);

        // Assert
        assertThat(vencido).isFalse();
    }

    @Test void TU04c_naoSinalizaPrevistoComPrazoNulo() {
        // Act
        var vencido = verificador.vencido(StatusEdital.PREVISTO, null);

        // Assert
        assertThat(vencido).isFalse();
    }
}
