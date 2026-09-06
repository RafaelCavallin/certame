package br.com.certame.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EnviadorEmailSmtpTest {
    private final Usuario usuario = new Usuario("Pessoa", "pessoa@certame.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);

    @Test void TU39_enviaRedefinicaoDeSenhaComSucessoNaPrimeiraTentativa() {
        // Arrange
        var remetente = mock(JavaMailSender.class);
        var enviador = new EnviadorEmailSmtp(remetente, "naoresponda@certame.local");

        // Act
        enviador.enviarRedefinicaoSenha(usuario, "https://certame.local/redefinir?token=abc");

        // Assert
        var mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remetente, times(1)).send(mensagem.capture());
        assertThat(mensagem.getValue().getTo()).containsExactly("pessoa@certame.com");
        assertThat(mensagem.getValue().getSubject()).isEqualTo("Redefinição de senha do Certame");
        assertThat(mensagem.getValue().getText()).contains("https://certame.local/redefinir?token=abc");
    }

    @Test void TU40_reenviaConviteEEntregaComSucessoAposUmaFalha() {
        // Arrange
        var remetente = mock(JavaMailSender.class);
        doThrow(new MailSendException("falha temporária")).doNothing().when(remetente).send(any(SimpleMailMessage.class));
        var enviador = new EnviadorEmailSmtp(remetente, "naoresponda@certame.local");

        // Act
        enviador.enviarConvite(usuario, "https://certame.local/convite?token=xyz");

        // Assert
        verify(remetente, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test void TU41_esgotaAsTresTentativasERegistraFalhaSemPropagarExcecao() {
        // Arrange
        var remetente = mock(JavaMailSender.class);
        doThrow(new MailSendException("indisponível")).when(remetente).send(any(SimpleMailMessage.class));
        var enviador = new EnviadorEmailSmtp(remetente, "naoresponda@certame.local");

        // Act
        assertThatCode(() -> enviador.enviarRedefinicaoSenha(usuario, "https://certame.local/redefinir?token=abc"))
                .doesNotThrowAnyException();

        // Assert
        verify(remetente, times(3)).send(any(SimpleMailMessage.class));
    }
}
