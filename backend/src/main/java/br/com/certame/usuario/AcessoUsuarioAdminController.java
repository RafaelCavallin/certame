package br.com.certame.usuario;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ações administrativas sobre o acesso de um usuário, separadas do CRUD de UsuarioAdminController. */
@RestController
@RequestMapping("/api/v1/super/usuarios")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AcessoUsuarioAdminController {
    private final AcessoUsuarioAdminService acesso;

    public AcessoUsuarioAdminController(AcessoUsuarioAdminService acesso) {
        this.acesso = acesso;
    }

    @PostMapping("/{id}/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@PathVariable UUID id) {
        acesso.redefinirSenha(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reenviar-convite")
    public ResponseEntity<Void> reenviarConvite(@PathVariable UUID id) {
        acesso.reenviarConvite(id);
        return ResponseEntity.noContent().build();
    }
}
