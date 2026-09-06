package br.com.certame.simulado;

import br.com.certame.simulado.dto.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimuladoCatalogoServiceTest {
    private final SimuladoRepository simulados = mock(SimuladoRepository.class);
    private final TentativaSimuladoRepository tentativas = mock(TentativaSimuladoRepository.class);
    private final JanelaSimulado janela = new JanelaSimulado(Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC));
    private final SimuladoCatalogoService service = new SimuladoCatalogoService(simulados, tentativas, janela);

    @Test void TU01_agrupaSimuladosEmDisponiveisAgendadosEConcluidos() {
        var disponivel = simulado("aberto", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"));
        var agendado = simulado("futuro", Instant.parse("2026-09-04T00:00:00Z"), Instant.parse("2026-09-05T00:00:00Z"));
        when(simulados.findByStatusAndExcluidoEmIsNull(StatusSimulado.PUBLICADO)).thenReturn(List.of(disponivel, agendado));
        when(tentativas.findByUsuarioIdAndStatus(any(), eq(StatusTentativa.EM_ANDAMENTO))).thenReturn(List.of());
        when(tentativas.findByUsuarioIdAndStatus(any(), eq(StatusTentativa.FINALIZADA))).thenReturn(List.of());

        var resposta = service.listarDoEstudante(UUID.randomUUID());

        assertThat(resposta.disponiveis()).extracting(SimuladoResumo::nome).containsExactly("aberto");
        assertThat(resposta.agendados()).extracting(SimuladoResumo::nome).containsExactly("futuro");
        assertThat(resposta.proximo().nome()).isEqualTo("futuro");
    }

    @Test void TU02_exibeNotaEDataNaListaDeConcluidos() {
        var simulado = simulado("concluido", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"));
        var tentativa = mock(TentativaSimulado.class);
        when(tentativa.getTipo()).thenReturn(TipoTentativa.VALIDA); when(tentativa.getStatus()).thenReturn(StatusTentativa.FINALIZADA);
        when(tentativa.getSimulado()).thenReturn(simulado); when(tentativa.getNota()).thenReturn((short) 80); when(tentativa.getFinalizadaEm()).thenReturn(Instant.parse("2026-09-02T13:00:00Z"));
        when(tentativas.findByUsuarioIdAndStatus(any(), eq(StatusTentativa.EM_ANDAMENTO))).thenReturn(List.of());
        when(tentativas.findByUsuarioIdAndStatus(any(), eq(StatusTentativa.FINALIZADA))).thenReturn(List.of(tentativa));
        when(simulados.findByStatusAndExcluidoEmIsNull(StatusSimulado.PUBLICADO)).thenReturn(List.of());

        var concluido = service.listarDoEstudante(UUID.randomUUID()).concluidos().getFirst();

        assertThat(concluido.nota()).isEqualTo((short) 80); assertThat(concluido.concluidoEm()).isEqualTo(Instant.parse("2026-09-02T13:00:00Z"));
    }

    private Simulado simulado(String nome, Instant abertura, Instant encerramento) {
        var simulado = mock(Simulado.class); when(simulado.getId()).thenReturn(UUID.randomUUID()); when(simulado.getNome()).thenReturn(nome); when(simulado.getStatus()).thenReturn(StatusSimulado.PUBLICADO); when(simulado.getJanelaAbertura()).thenReturn(abertura); when(simulado.getJanelaEncerramento()).thenReturn(encerramento); when(simulado.getNumeroQuestoes()).thenReturn((short) 10); when(simulado.getDuracaoSegundos()).thenReturn(600); return simulado;
    }
}
