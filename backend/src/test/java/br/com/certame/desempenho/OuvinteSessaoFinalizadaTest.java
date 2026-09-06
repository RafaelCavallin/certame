package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import br.com.certame.comum.evento.SessaoFinalizada;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OuvinteSessaoFinalizadaTest {
    @Test void TU35_registraPendenciaSemPropagarFalhaDoAgregado() {
        var usuarioId = UUID.randomUUID();
        var atualizador = mock(AtualizadorAgregados.class);
        var pendencias = mock(DesempenhoPendenciaRepository.class);
        doThrow(new IllegalStateException("falha")).when(atualizador).atualizar(usuarioId);
        var ouvinte = new OuvinteSessaoFinalizada(atualizador, pendencias, new SimpleMeterRegistry());
        assertThatCode(() -> ouvinte.aoFinalizar(new SessaoFinalizada(usuarioId, UUID.randomUUID(), Instant.EPOCH))).doesNotThrowAnyException();
        verify(pendencias).save(argThat(pendencia -> pendencia.getUsuarioId().equals(usuarioId)));
    }
}
