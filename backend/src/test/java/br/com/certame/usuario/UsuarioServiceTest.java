package br.com.certame.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.comum.excecao.EmailEmUsoException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioServiceTest {
    private final UsuarioRepository usuarios = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioService service = new UsuarioService(usuarios, encoder);

    @Test
    void TU05_cadastraEstudanteComEmailInedito() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("pessoa@example.com")).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario usuario = service.cadastrarEstudante(new RequisicaoCadastro(
                " Pessoa Teste ", "Pessoa@Example.com", "senhaSegura"));

        assertThat(usuario.getNome()).isEqualTo("Pessoa Teste");
        assertThat(usuario.getEmail()).isEqualTo("pessoa@example.com");
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.ESTUDANTE);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(encoder.matches("senhaSegura", usuario.getSenhaHash())).isTrue();
        verify(usuarios).saveAndFlush(any(Usuario.class));
    }

    @Test
    void TU06_rejeitaEmailExistente() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("pessoa@example.com"))
                .thenReturn(Optional.of(new Usuario("Pessoa Teste", "pessoa@example.com", "hash",
                        PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO)));

        assertThatThrownBy(() -> service.cadastrarEstudante(new RequisicaoCadastro(
                "Pessoa Teste", "Pessoa@Example.com", "senhaSegura")))
                .isInstanceOf(EmailEmUsoException.class);
    }

    @Test
    void TU07_normalizaEmailDoCadastro() {
        when(usuarios.findByEmailAndExcluidoEmIsNull("rafael@x.com")).thenReturn(Optional.empty());
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertThat(service.cadastrarEstudante(new RequisicaoCadastro(
                "Rafael Cavallin", " Rafael@X.com ", "senhaSegura")).getEmail())
                .isEqualTo("rafael@x.com");
    }
}
