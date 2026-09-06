package br.com.certame.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import br.com.certame.usuario.UsuarioService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AutenticacaoCadastroServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final TokenService tokens = mock(TokenService.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final LimitadorTentativas limitador = mock(LimitadorTentativas.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final AutenticacaoService service = new AutenticacaoService(
            usuarios, encoder, tokens, refresh, limitador, usuarioService);

    @Test
    void TU05_cadastraEAbreSessao() {
        var usuario = new Usuario("Pessoa Teste", "pessoa@example.com", "hash",
                PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var access = new TokenService.AccessToken("jwt", Instant.EPOCH);
        when(usuarioService.cadastrarEstudante(any())).thenReturn(usuario);
        when(tokens.emitirAccessToken(usuario)).thenReturn(access);
        when(refresh.emitir(usuario, ContextoRequisicao.vazio())).thenReturn("refresh");

        var resultado = service.cadastrarEstudante(
                new RequisicaoCadastro("Pessoa Teste", "Pessoa@Example.com", "senhaSegura"),
                ContextoRequisicao.vazio());

        assertThat(resultado.resposta().usuario().perfil()).isEqualTo("ESTUDANTE");
        verify(usuarioService).cadastrarEstudante(any());
        verify(limitador).registrarSucesso("pessoa@example.com");
    }
}
