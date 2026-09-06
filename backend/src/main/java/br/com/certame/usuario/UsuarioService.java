package br.com.certame.usuario;

import br.com.certame.auth.dto.RequisicaoCadastro;
import br.com.certame.comum.excecao.EmailEmUsoException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de escrita da entidade Usuario, incluindo o cadastro de RF-02. */
@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarEstudante(RequisicaoCadastro requisicao) {
        String email = normalizarEmail(requisicao.email());
        if (usuarios.findByEmailAndExcluidoEmIsNull(email).isPresent()) {
            throw new EmailEmUsoException();
        }

        var usuario = new Usuario(requisicao.nome().trim(), email,
                passwordEncoder.encode(requisicao.senha()), PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        try {
            return usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException excecao) {
            throw new EmailEmUsoException();
        }
    }

    @Transactional
    public void trocarSenha(Usuario usuario, String senha) {
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuarios.save(usuario);
    }

    public String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
