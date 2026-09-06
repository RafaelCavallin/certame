package br.com.certame.auth;

import br.com.certame.config.PropriedadesSeguranca;
import br.com.certame.email.EnviadorEmail;
import br.com.certame.usuario.Usuario;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Emite (grava e envia por e-mail) um token de redefinição de senha ou de convite. */
@Component
public class EmissorTokenSenha {
    private final TokenSenhaRepository tokens;
    private final PropriedadesSeguranca propriedades;
    private final EnviadorEmail enviador;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmissorTokenSenha(TokenSenhaRepository tokens, PropriedadesSeguranca propriedades,
            EnviadorEmail enviador, Clock clock) {
        this.tokens = tokens;
        this.propriedades = propriedades;
        this.enviador = enviador;
        this.clock = clock;
    }

    public void emitir(Usuario usuario, FinalidadeTokenSenha finalidade, boolean convite) {
        Instant agora = clock.instant();
        tokens.invalidarPendentes(usuario.getId(), finalidade, agora);
        String bruto = gerarTokenBruto();
        tokens.save(new TokenSenha(usuario, RefreshTokenService.hash(bruto), finalidade,
                agora.plus(propriedades.getTokenSenhaTtl())));
        agendarEnvio(usuario, bruto, convite);
    }

    private String gerarTokenBruto() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void agendarEnvio(Usuario usuario, String bruto, boolean convite) {
        String link = propriedades.getFrontendUrl().replaceAll("/$", "") + "/definir-senha/" + bruto;
        Runnable envio = () -> {
            if (convite) {
                enviador.enviarConvite(usuario, link);
            } else {
                enviador.enviarRedefinicaoSenha(usuario, link);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            envio.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                envio.run();
            }
        });
    }
}
