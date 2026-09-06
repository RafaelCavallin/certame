package br.com.certame.auth.dto;

import br.com.certame.usuario.Usuario;
import java.util.UUID;

public record UsuarioResumo(UUID id, String nome, String email, String perfil) {
    public static UsuarioResumo de(Usuario usuario) {
        return new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil().name());
    }
}
