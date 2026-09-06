package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.comum.excecao.TokenInvalidoException;
import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.email.EnviadorEmail;
import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import br.com.certame.usuario.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RedefinicaoSenhaServiceTest {
    private static final Instant AGORA = Instant.parse("2026-08-30T12:00:00Z");
    private final UsuarioRepository usuarios = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final TokenSenhaRepository tokens = org.mockito.Mockito.mock(TokenSenhaRepository.class);
    private final UsuarioService usuarioService = org.mockito.Mockito.mock(UsuarioService.class);
    private final RefreshTokenService refreshTokens = org.mockito.Mockito.mock(RefreshTokenService.class);
    private final EnviadorEmail enviador = org.mockito.Mockito.mock(EnviadorEmail.class);
    private final LimitadorTentativas limitador = org.mockito.Mockito.mock(LimitadorTentativas.class);
    private final PropriedadesSeguranca propriedades = new PropriedadesSeguranca();
    private RedefinicaoSenhaService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        propriedades.setFrontendUrl("https://app.certame.test");
        usuario = new Usuario("Pessoa Teste", "pessoa@example.com", "hash",
                PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var emissor = new EmissorTokenSenha(tokens, propriedades, enviador, Clock.fixed(AGORA, ZoneOffset.UTC));
        service = new RedefinicaoSenhaService(usuarios, tokens, usuarioService, refreshTokens, limitador, emissor,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void TU08_naoGeraTokenParaEmailInexistente() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("ausente@example.com")).thenReturn(Optional.empty());

        service.solicitar("Ausente@Example.com");

        org.mockito.Mockito.verifyNoInteractions(enviador);
        org.mockito.Mockito.verify(tokens, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void TU09_geraTokenComHashEEnviaLinkParaEmailExistente() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("pessoa@example.com")).thenReturn(Optional.of(usuario));
        when(tokens.save(any(TokenSenha.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.solicitar(" pessoa@example.com ");

        ArgumentCaptor<TokenSenha> captor = ArgumentCaptor.forClass(TokenSenha.class);
        verify(tokens).save(captor.capture());
        TokenSenha token = captor.getValue();
        assertThat(token.getTokenHash()).hasSize(44);
        assertThat(token.getExpiraEm()).isEqualTo(AGORA.plusSeconds(3600));
        org.mockito.Mockito.verify(enviador).enviarRedefinicaoSenha(any(), org.mockito.ArgumentMatchers.contains("/definir-senha/"));
    }

    @Test
    void TU10_redefineSenhaERevogaSessoes() {
        TokenSenha token = token(FinalidadeTokenSenha.REDEFINICAO, AGORA.plusSeconds(60));
        when(tokens.findByTokenHash(RefreshTokenService.hash("token-valido"))).thenReturn(Optional.of(token));

        service.redefinir("token-valido", "novaSenha");

        verify(usuarioService).trocarSenha(usuario, "novaSenha");
        verify(tokens).save(token);
        verify(refreshTokens).revogarTodosDoUsuario(usuario.getId());
        assertThat(token.getUsadoEm()).isEqualTo(AGORA);
    }

    @Test
    void TU11_rejeitaTokenExpirado() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(token(FinalidadeTokenSenha.REDEFINICAO, AGORA)));

        assertThatThrownBy(() -> service.redefinir("expirado", "novaSenha"))
                .isInstanceOf(TokenInvalidoException.class);
        org.mockito.Mockito.verifyNoInteractions(usuarioService, refreshTokens);
    }

    @Test
    void TU12_rejeitaTokenConsumido() {
        TokenSenha token = token(FinalidadeTokenSenha.REDEFINICAO, AGORA.plusSeconds(60));
        token.setUsadoEm(AGORA.minusSeconds(1));
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.redefinir("consumido", "novaSenha"))
                .isInstanceOf(TokenInvalidoException.class);
        org.mockito.Mockito.verifyNoInteractions(usuarioService, refreshTokens);
    }

    @Test
    void TU13_conviteAtivaUsuarioAoDefinirSenha() {
        usuario.setStatus(StatusUsuario.CONVITE_PENDENTE);
        TokenSenha token = token(FinalidadeTokenSenha.CONVITE, AGORA.plusSeconds(60));
        when(tokens.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.redefinir("convite", "novaSenha");

        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        verify(usuarioService).trocarSenha(usuario, "novaSenha");
    }

    private TokenSenha token(FinalidadeTokenSenha finalidade, Instant expiraEm) {
        return new TokenSenha(usuario, "hash", finalidade, expiraEm);
    }
}
