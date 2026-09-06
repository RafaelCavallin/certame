package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.usuario.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class TokenServiceTest {
    @Test void TU16_emiteClaimsEExpiraEm15Minutos() throws Exception {
        var encoder = mock(JwtEncoder.class); var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("jwt"); when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        var agora = Instant.parse("2026-01-01T00:00:00Z"); var props = new PropriedadesSeguranca();
        var usuario = new Usuario("Pessoa", "p@e.com", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        var campoId = Usuario.class.getDeclaredField("id"); campoId.setAccessible(true); campoId.set(usuario, UUID.randomUUID());
        var service = new TokenService(encoder, props, Clock.fixed(agora, ZoneOffset.UTC));
        var access = service.emitirAccessToken(usuario);
        assertThat(access.valor()).isEqualTo("jwt"); assertThat(access.expiraEm()).isEqualTo(agora.plusSeconds(900));
        verify(encoder).encode(any(JwtEncoderParameters.class));
    }
}
