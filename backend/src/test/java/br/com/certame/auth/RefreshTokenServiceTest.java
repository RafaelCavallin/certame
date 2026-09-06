package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.certame.comum.excecao.SessaoExpiradaException;
import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.usuario.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {
    @Test void TU14_consumeTokenValidoMarcaComoUsado() {
        var repo = mock(RefreshTokenRepository.class); var usuario = new Usuario("Pessoa", "p@e.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var agora = Instant.parse("2026-01-01T00:00:00Z"); var token = new RefreshToken(usuario, RefreshTokenService.hash("bruto"), agora.plusSeconds(100), null, null);
        when(repo.findByTokenHash(RefreshTokenService.hash("bruto"))).thenReturn(Optional.of(token));
        new RefreshTokenService(repo, new PropriedadesSeguranca(), Clock.fixed(agora, ZoneOffset.UTC)).consumirRotacionando("bruto");
        org.assertj.core.api.Assertions.assertThat(token.getUsadoEm()).isEqualTo(agora);
    }

    @Test void TU15_reusoRevogaTokensEFalha() {
        var repo = mock(RefreshTokenRepository.class); var usuario = new Usuario("Pessoa", "p@e.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var token = new RefreshToken(usuario, RefreshTokenService.hash("bruto"), Instant.MAX, null, null); token.setUsadoEm(Instant.EPOCH);
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(token)); when(repo.findAllByUsuarioId(any())).thenReturn(List.of(token));
        var service = new RefreshTokenService(repo, new PropriedadesSeguranca());
        assertThatThrownBy(() -> service.consumirRotacionando("bruto")).isInstanceOf(SessaoExpiradaException.class);
        verify(repo).flush();
    }
}
