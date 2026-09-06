package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtualizadorAgregadosTest {
    private final UUID usuarioId = UUID.randomUUID();

    @Test void TU31_fazUpsertDosTotaisComPercentualArredondado() {
        var fonte = mock(FonteRespostas.class);
        var semanais = mock(DesempenhoSemanalRepository.class);
        var disciplinas = mock(DesempenhoDisciplinaRepository.class);
        var assuntos = mock(DesempenhoAssuntoRepository.class);
        when(fonte.porSemana(usuarioId)).thenReturn(List.of(new FonteRespostas.TotalPorChave<>(LocalDate.of(2026, 8, 24), 3, 2)));
        when(fonte.porDisciplina(usuarioId)).thenReturn(List.of(new FonteRespostas.TotalPorDisciplina(UUID.randomUUID(), 5, 3)));
        when(fonte.porAssunto(usuarioId)).thenReturn(List.of());
        new AtualizadorAgregados(fonte, semanais, disciplinas, assuntos, new SimpleMeterRegistry()).atualizar(usuarioId);
        verify(semanais).upsert(eq(usuarioId), eq(LocalDate.of(2026, 8, 24)), eq(3), eq(2), eq((short) 67));
        verify(disciplinas).upsert(eq(usuarioId), any(), eq(5), eq(3), eq((short) 60));
    }

    @Test void TU32_reexecutaSubstituindoTotaisSemDuplicar() {
        var fonte = mock(FonteRespostas.class);
        var semanais = mock(DesempenhoSemanalRepository.class);
        var disciplinas = mock(DesempenhoDisciplinaRepository.class);
        var assuntos = mock(DesempenhoAssuntoRepository.class);
        when(fonte.porSemana(usuarioId)).thenReturn(List.of(new FonteRespostas.TotalPorChave<>(LocalDate.of(2026, 8, 24), 2, 1)));
        when(fonte.porDisciplina(usuarioId)).thenReturn(List.of());
        when(fonte.porAssunto(usuarioId)).thenReturn(List.of());
        var atualizador = new AtualizadorAgregados(fonte, semanais, disciplinas, assuntos, new SimpleMeterRegistry());
        atualizador.atualizar(usuarioId);
        atualizador.atualizar(usuarioId);
        verify(semanais, times(2)).excluirDoUsuario(usuarioId);
        verify(semanais, times(2)).upsert(eq(usuarioId), eq(LocalDate.of(2026, 8, 24)), eq(2), eq(1), eq((short) 50));
    }

    @Test void TU33_naoIncluiRespostaDeSessaoNaoFinalizada() {
        var fonte = mock(FonteRespostas.class);
        when(fonte.porSemana(usuarioId)).thenReturn(List.of());
        when(fonte.porDisciplina(usuarioId)).thenReturn(List.of());
        when(fonte.porAssunto(usuarioId)).thenReturn(List.of());
        var semanais = mock(DesempenhoSemanalRepository.class);
        new AtualizadorAgregados(fonte, semanais, mock(DesempenhoDisciplinaRepository.class), mock(DesempenhoAssuntoRepository.class), new SimpleMeterRegistry()).atualizar(usuarioId);
        verify(semanais).excluirDoUsuario(usuarioId);
        verify(semanais, never()).upsert(any(), any(), anyInt(), anyInt(), anyShort());
    }

    @Test void TU34_mantemAssuntoSemRegistroQuandoNaoHaAssunto() {
        var fonte = mock(FonteRespostas.class);
        var disciplinaId = UUID.randomUUID();
        when(fonte.porSemana(usuarioId)).thenReturn(List.of());
        when(fonte.porDisciplina(usuarioId)).thenReturn(List.of(new FonteRespostas.TotalPorDisciplina(disciplinaId, 1, 0)));
        when(fonte.porAssunto(usuarioId)).thenReturn(List.of());
        var assuntos = mock(DesempenhoAssuntoRepository.class);
        new AtualizadorAgregados(fonte, mock(DesempenhoSemanalRepository.class), mock(DesempenhoDisciplinaRepository.class), assuntos, new SimpleMeterRegistry()).atualizar(usuarioId);
        verify(assuntos, never()).upsert(any(), any(), any(), anyInt(), anyInt(), anyShort());
    }
}
