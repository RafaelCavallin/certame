package br.com.certame.usuario.dto;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import br.com.certame.usuario.Usuario;
import java.time.Instant;
import java.util.UUID;

public record UsuarioGestaoResumo(UUID id, String nome, String email, PerfilUsuario perfil,
                                  StatusUsuario status, Instant criadoEm) {

    public static UsuarioGestaoResumo de(Usuario usuario) {
        return new UsuarioGestaoResumo(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.getStatus(), usuario.getCriadoEm());
    }
}
