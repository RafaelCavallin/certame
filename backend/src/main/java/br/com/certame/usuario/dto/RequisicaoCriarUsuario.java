package br.com.certame.usuario.dto;

import br.com.certame.usuario.PerfilUsuario;
import br.com.certame.usuario.StatusUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequisicaoCriarUsuario(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.") String nome,
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.") String email,
        @NotNull(message = "Selecione o perfil.") PerfilUsuario perfil,
        @NotNull(message = "Selecione o status.") StatusUsuario status,
        Boolean enviarConvite) {

    public RequisicaoCriarUsuario {
        enviarConvite = enviarConvite == null || enviarConvite;
    }
}
