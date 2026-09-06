package br.com.certame.estudo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpiradorSessoesTest {
    private static final Instant AGORA = Instant.parse("2026-08-31T15:00:00Z");
    private final SessaoEstudoRepository sessoes = mock(SessaoEstudoRepository.class);
    private final ExpiradorSessoes expirador = new ExpiradorSessoes(sessoes, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test void TU40_expiraProvaApos24HorasELeituraAposSeteDias() {
        var prova = sessao(ModoSessao.PROVA, AGORA.minus(Duration.ofHours(25)));
        var leitura = sessao(ModoSessao.FOCO, AGORA.minus(Duration.ofDays(8)));
        var recente = sessao(ModoSessao.CONTEXTO, AGORA.minus(Duration.ofDays(6)));
        when(sessoes.buscarRetomaveisExpiradas(any())).thenReturn(List.of(prova, leitura, recente));

        expirador.expirar();

        verify(prova).setExpiradaEm(AGORA);
        verify(leitura).setExpiradaEm(AGORA);
        verify(recente, never()).setExpiradaEm(any());
    }
    private SessaoEstudo sessao(ModoSessao modo, Instant atividade) { var sessao = mock(SessaoEstudo.class); when(sessao.getModo()).thenReturn(modo); when(sessao.getUltimaAtividadeEm()).thenReturn(atividade); return sessao; }
}
