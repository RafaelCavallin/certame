package br.com.certame.usuario;

import br.com.certame.comum.excecao.ConflitoException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Impede que um super-admin rebaixe/desative a própria conta ou remova o último super-admin ativo. */
@Service
public class SalvaguardaUsuarioService {
    private static final String MSG_AUTO_MODIFICACAO = "Você não pode rebaixar ou desativar o próprio acesso.";
    private static final String MSG_ULTIMO_SUPER_ADMIN =
            "O último super-admin ativo não pode ser rebaixado ou desativado.";
    private final UsuarioRepository usuarios;

    public SalvaguardaUsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public void garantirEdicao(Usuario usuario, UUID usuarioId, UUID atorId, PerfilUsuario perfilNovo,
            StatusUsuario statusNovo) {
        if (!rebaixaOuDesativa(usuario, perfilNovo, statusNovo)) return;
        garantirExclusao(usuario, usuarioId, atorId);
    }

    public void garantirExclusao(Usuario usuario, UUID usuarioId, UUID atorId) {
        if (usuarioId.equals(atorId)) throw new ConflitoException("AUTO_MODIFICACAO", MSG_AUTO_MODIFICACAO);
        if (ultimoSuperAdminAtivo(usuario)) throw new ConflitoException("ULTIMO_SUPER_ADMIN", MSG_ULTIMO_SUPER_ADMIN);
    }

    private boolean rebaixaOuDesativa(Usuario usuario, PerfilUsuario perfilNovo, StatusUsuario statusNovo) {
        boolean rebaixaPerfil = usuario.getPerfil() == PerfilUsuario.SUPER_ADMIN && perfilNovo != PerfilUsuario.SUPER_ADMIN;
        boolean desativa = usuario.getStatus() == StatusUsuario.ATIVO && statusNovo != StatusUsuario.ATIVO;
        return rebaixaPerfil || desativa;
    }

    private boolean ultimoSuperAdminAtivo(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.SUPER_ADMIN && usuario.getStatus() == StatusUsuario.ATIVO
                && usuarios.countByPerfilAndStatusAndExcluidoEmIsNull(PerfilUsuario.SUPER_ADMIN, StatusUsuario.ATIVO) <= 1;
    }
}
