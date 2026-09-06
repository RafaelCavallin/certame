package br.com.certame.email;

import br.com.certame.usuario.Usuario;
import io.micrometer.core.instrument.Metrics;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EnviadorEmailSmtp implements EnviadorEmail {
    private static final Logger log = LoggerFactory.getLogger(EnviadorEmailSmtp.class);
    private static final int MAX_TENTATIVAS = 3;
    private static final long ESPERA_PRIMEIRA_TENTATIVA_MS = 1_000L;
    private static final long ESPERA_SEGUNDA_TENTATIVA_MS = 4_000L;
    private final JavaMailSender remetente;
    private final String emailOrigem;

    public EnviadorEmailSmtp(JavaMailSender remetente,
                             @Value("${spring.mail.username:noreply@certame.local}") String emailOrigem) {
        this.remetente = remetente;
        this.emailOrigem = emailOrigem;
    }

    @Override
    @Async
    public void enviarRedefinicaoSenha(Usuario usuario, String linkAbsoluto) {
        enviar(usuario, "Redefinição de senha do Certame",
                "Olá, " + usuario.getNome() + ",\n\n"
                        + "Recebemos um pedido para redefinir sua senha. Acesse o link abaixo em até 60 minutos:\n\n"
                        + linkAbsoluto + "\n\n"
                        + "Se você não fez este pedido, ignore esta mensagem.\n\nEquipe Certame");
    }

    @Override
    @Async
    public void enviarConvite(Usuario usuario, String linkAbsoluto) {
        enviar(usuario, "Seu convite para o Certame",
                "Olá, " + usuario.getNome() + ",\n\n"
                        + "Você foi convidado para o Certame. Defina sua senha pelo link abaixo em até 60 minutos:\n\n"
                        + linkAbsoluto + "\n\nEquipe Certame");
    }

    private void enviar(Usuario usuario, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(emailOrigem);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                remetente.send(mensagem);
                return;
            } catch (RuntimeException excecao) {
                if (tentativa < MAX_TENTATIVAS) {
                    try {
                        Thread.sleep(tentativa == 1 ? ESPERA_PRIMEIRA_TENTATIVA_MS : ESPERA_SEGUNDA_TENTATIVA_MS);
                    } catch (InterruptedException interrompido) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        Metrics.counter("certame.auth.email.falha").increment();
        log.error("Falha ao enviar e-mail transacional para usuarioId={}", usuario.getId());
    }
}
