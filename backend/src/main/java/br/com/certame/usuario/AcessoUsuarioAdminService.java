package br.com.certame.usuario;

import br.com.certame.auth.RedefinicaoSenhaService;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.StatusIncompativelException;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ações administrativas sobre o acesso de um usuário: redefinir senha e reenviar convite. */
@Service
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AcessoUsuarioAdminService {
    private final LocalizadorUsuarioAdmin localizador;
    private final RedefinicaoSenhaService redefinicaoSenha;
    private final RegistradorAuditoria auditoria;

    public AcessoUsuarioAdminService(LocalizadorUsuarioAdmin localizador, RedefinicaoSenhaService redefinicaoSenha,
            RegistradorAuditoria auditoria) {
        this.localizador = localizador;
        this.redefinicaoSenha = redefinicaoSenha;
        this.auditoria = auditoria;
    }

    @Transactional
    public void redefinirSenha(UUID usuarioId) {
        var usuario = localizador.buscarAtivo(usuarioId);
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new StatusIncompativelException("Só é possível redefinir a senha de um usuário ativo.");
        }
        redefinicaoSenha.emitirRedefinicao(usuario);
        auditoria.registrar("REDEFINIR_SENHA", "usuario", usuario.getId(), null);
    }

    @Transactional
    public void reenviarConvite(UUID usuarioId) {
        var usuario = localizador.buscarAtivo(usuarioId);
        if (usuario.getStatus() != StatusUsuario.CONVITE_PENDENTE) {
            throw new StatusIncompativelException("Só é possível reenviar convite a um usuário com convite pendente.");
        }
        redefinicaoSenha.emitirConvite(usuario);
        auditoria.registrar("REENVIAR_CONVITE", "usuario", usuario.getId(), null);
    }
}
