package br.com.certame.usuario;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Gera o hash de uma senha descartável para contas criadas sem convite por e-mail. */
@Service
public class GeradorSenhaTemporaria {
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public GeradorSenhaTemporaria(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String gerarHash() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return passwordEncoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
