package br.com.certame.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class ManipuladorErrosGlobalTest {
    private final ManipuladorErrosGlobal manipulador = new ManipuladorErrosGlobal();

    @Test
    void trataViolacaoDeIntegridadeComEnvelopeSeguro() {
        // Arrange
        var excecao = new DataIntegrityViolationException("detalhe interno da constraint");

        // Act
        var resposta = manipulador.tratarIntegridade(excecao);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resposta.getBody()).isEqualTo(new ErroApi(
                "INTEGRIDADE_INVALIDA", "Não foi possível concluir a operação com os dados informados.", null));
    }
}
