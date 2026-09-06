package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import br.com.certame.comum.excecao.MuitasTentativasException;
import br.com.certame.config.PropriedadesSeguranca;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitadorTentativasTest {
    @Test void TU17_bloqueiaSextaFalhaNaJanela() {
        var propriedades = new PropriedadesSeguranca();
        var limitador = new LimitadorTentativasMemoria(propriedades, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) limitador.registrarFalha("pessoa@example.com");
        assertThatThrownBy(() -> limitador.verificar("pessoa@example.com")).isInstanceOf(MuitasTentativasException.class);
    }
}
