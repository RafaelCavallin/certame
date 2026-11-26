package br.com.certame.importacao.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AvaliadorComentarioTest {
    private final AvaliadorComentario avaliador = new AvaliadorComentario();

    @Test
    void TU11_sinalizaComentarioAusenteSemDescartarCandidata() {
        assertThat(avaliador.avaliar(null, "Enunciado qualquer", "A")).isTrue();
        assertThat(avaliador.avaliar("   ", "Enunciado qualquer", "A")).isTrue();
    }

    @Test
    void TU12_reprovaComentarioApenasComALetraDoGabarito() {
        assertThat(avaliador.avaliar("CERTO", "O enunciado da questão de certo ou errado.", "CERTO")).isTrue();
    }

    @Test
    void TU13_reprovaComentarioQueRepeteOEnunciado() {
        var enunciado = "Qual é a capital da França?";

        assertThat(avaliador.avaliar(enunciado, enunciado, "A")).isTrue();
    }

    @Test
    void aceitaComentarioComConteudoProprio() {
        var comentario = "A resposta correta é A porque o enunciado trata do conceito de throughput.";

        assertThat(avaliador.avaliar(comentario, "Enunciado qualquer", "A")).isFalse();
    }
}
