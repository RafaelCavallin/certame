package br.com.certame.desempenho;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReconciliadorAgregadosTest {
    @Test void TU36_reconciliaPendenciaReconstruindoAgregadoELimpandoAFila() {
        // Arrange
        var usuarioId = UUID.randomUUID();
        var pendencia = new DesempenhoPendencia(usuarioId);
        var pendencias = mock(DesempenhoPendenciaRepository.class);
        var atualizador = mock(AtualizadorAgregados.class);
        when(pendencias.findAllByOrderByMarcadoEmAsc()).thenReturn(List.of(pendencia));
        var reconciliador = new ReconciliadorAgregados(pendencias, atualizador, new SimpleMeterRegistry());

        // Act
        reconciliador.reconciliar();

        // Assert
        verify(atualizador).atualizar(usuarioId);
        verify(pendencias).deleteById(usuarioId);
    }

    @Test void TU37_falhaEmUmItemDoLoteNaoImpedeProcessamentoDosDemais() {
        // Arrange
        var usuarioComFalha = UUID.randomUUID();
        var usuarioComSucesso = UUID.randomUUID();
        var pendenciaComFalha = new DesempenhoPendencia(usuarioComFalha);
        var pendenciaComSucesso = new DesempenhoPendencia(usuarioComSucesso);
        var pendencias = mock(DesempenhoPendenciaRepository.class);
        var atualizador = mock(AtualizadorAgregados.class);
        when(pendencias.findAllByOrderByMarcadoEmAsc()).thenReturn(List.of(pendenciaComFalha, pendenciaComSucesso));
        doThrow(new RuntimeException("divergência")).when(atualizador).atualizar(usuarioComFalha);
        var reconciliador = new ReconciliadorAgregados(pendencias, atualizador, new SimpleMeterRegistry());

        // Act
        reconciliador.reconciliar();

        // Assert
        verify(atualizador).atualizar(usuarioComSucesso);
        verify(pendencias).deleteById(usuarioComSucesso);
        verify(pendencias, never()).deleteById(usuarioComFalha);
    }

    @Test void TU38_loteRespeitaLimiteDeCemItensPorExecucao() {
        // Arrange
        var pendencias = mock(DesempenhoPendenciaRepository.class);
        var atualizador = mock(AtualizadorAgregados.class);
        var lote = java.util.stream.Stream.generate(() -> new DesempenhoPendencia(UUID.randomUUID()))
                .limit(150).toList();
        when(pendencias.findAllByOrderByMarcadoEmAsc()).thenReturn(lote);
        var reconciliador = new ReconciliadorAgregados(pendencias, atualizador, new SimpleMeterRegistry());

        // Act
        reconciliador.reconciliar();

        // Assert
        verify(atualizador, times(100)).atualizar(any());
    }
}
