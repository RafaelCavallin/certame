package br.com.certame.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.catalogo.Disciplina;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidadorPublicacaoSimuladoTest {
    private static final Instant ABERTURA = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant ENCERRAMENTO = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void TU05_recusaPublicacaoSemJanela() {
        var simulado = simulado(10, null, null);

        var pendencias = new ValidadorPublicacaoSimulado().validar(simulado, List.of(), Map.of());

        assertThat(pendencias).containsEntry("janelaAbertura", "Defina a janela de aplicação antes de publicar.");
    }

    @Test
    void TU06_recusaPublicacaoComComposicaoQueNaoFecha() {
        var disciplina = disciplina("Direito");
        var simulado = simulado(80, ABERTURA, ENCERRAMENTO);

        var pendencias = new ValidadorPublicacaoSimulado().validar(simulado,
                List.of(composicao(disciplina, 50)), Map.of(disciplina.getId(), 50L));

        assertThat(pendencias).containsEntry("composicao", "A composição soma 50 questões, mas o simulado declara 80.");
    }

    @Test
    void TU07_recusaPublicacaoComAcervoInsuficiente() {
        var disciplina = disciplina("Português");
        var simulado = simulado(20, ABERTURA, ENCERRAMENTO);

        var pendencias = new ValidadorPublicacaoSimulado().validar(simulado,
                List.of(composicao(disciplina, 20)), Map.of(disciplina.getId(), 12L));

        assertThat(pendencias).containsEntry("composicao.Português",
                "A disciplina exige 20 questões e há apenas 12 publicadas.");
    }

    private Simulado simulado(int quantidade, Instant abertura, Instant encerramento) {
        var simulado = mock(Simulado.class);
        when(simulado.getNumeroQuestoes()).thenReturn((short) quantidade);
        when(simulado.getJanelaAbertura()).thenReturn(abertura);
        when(simulado.getJanelaEncerramento()).thenReturn(encerramento);
        return simulado;
    }

    private Disciplina disciplina(String nome) {
        var disciplina = mock(Disciplina.class);
        when(disciplina.getId()).thenReturn(UUID.nameUUIDFromBytes(nome.getBytes()));
        when(disciplina.getNome()).thenReturn(nome);
        return disciplina;
    }

    private SimuladoComposicao composicao(Disciplina disciplina, int quantidade) {
        var item = mock(SimuladoComposicao.class);
        when(item.getDisciplina()).thenReturn(disciplina);
        when(item.getQuantidade()).thenReturn((short) quantidade);
        return item;
    }
}
