package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import br.com.certame.auth.dto.RequisicaoLogin;
import br.com.certame.comum.excecao.CredencialInvalidaException;
import br.com.certame.usuario.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AutenticacaoServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TokenService tokens = mock(TokenService.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final LimitadorTentativas limitador = mock(LimitadorTentativas.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final AutenticacaoService service = new AutenticacaoService(usuarios, encoder, tokens, refresh, limitador, usuarioService);

    @Test void TU01_autenticaUsuarioAtivo() {
        var usuario = new Usuario("Pessoa Teste", "pessoa@example.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        when(usuarios.findByEmailAndExcluidoEmIsNull("pessoa@example.com")).thenReturn(Optional.of(usuario));
        when(encoder.matches("senhaSegura", "hash")).thenReturn(true);
        when(tokens.emitirAccessToken(usuario)).thenReturn(new TokenService.AccessToken("jwt", java.time.Instant.EPOCH));
        when(refresh.emitir(eq(usuario), any())).thenReturn("refresh");
        service.autenticar(new RequisicaoLogin("Pessoa@Example.com", "senhaSegura"), ContextoRequisicao.vazio());
        verify(refresh).emitir(eq(usuario), any());
    }

    @Test void TU02_rejeitaSenhaIncorreta() {
        var usuario = new Usuario("Pessoa Teste", "pessoa@example.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.of(usuario));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.autenticar(new RequisicaoLogin("pessoa@example.com", "senhaSegura"), ContextoRequisicao.vazio())).isInstanceOf(CredencialInvalidaException.class);
    }

    @Test void TU03_eTU04_rejeitamStatusNaoAtivo() {
        for (var status : new StatusUsuario[] { StatusUsuario.INATIVO, StatusUsuario.CONVITE_PENDENTE }) {
            var usuario = new Usuario("Pessoa Teste", "pessoa@example.com", "hash", PerfilUsuario.ESTUDANTE, status);
            when(usuarios.findByEmailAndExcluidoEmIsNull(anyString())).thenReturn(Optional.of(usuario));
            when(encoder.matches(anyString(), anyString())).thenReturn(true);
            assertThatThrownBy(() -> service.autenticar(new RequisicaoLogin("pessoa@example.com", "senhaSegura"), ContextoRequisicao.vazio())).isInstanceOf(CredencialInvalidaException.class);
        }
    }
}
