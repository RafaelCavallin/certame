package br.com.certame.usuario.dto;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;

public record FiltroUsuarios(PerfilUsuario perfil, StatusUsuario status) {
}
