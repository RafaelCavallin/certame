package br.com.certame.usuario;

import br.com.certame.comum.excecao.EmailEmUsoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Localiza e valida a identidade de um usuário-alvo para as operações administrativas (RN-01). */
@Service
public class LocalizadorUsuarioAdmin {
    private final UsuarioRepository usuarios;

    public LocalizadorUsuarioAdmin(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario buscarAtivo(UUID usuarioId) {
        return usuarios.findByIdAndExcluidoEmIsNull(usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }

    public void garantirEmailInedito(String email, UUID usuarioIdAtual) {
        usuarios.findByEmailAndExcluidoEmIsNull(email)
                .filter(encontrado -> usuarioIdAtual == null || !encontrado.getId().equals(usuarioIdAtual))
                .ifPresent(encontrado -> { throw new EmailEmUsoException(); });
    }

    public String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
