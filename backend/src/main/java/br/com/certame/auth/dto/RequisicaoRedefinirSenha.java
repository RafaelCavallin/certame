package br.com.certame.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoRedefinirSenha(
        @NotBlank(message = "Informe o token de redefinição.") String token,
        @NotBlank(message = "Informe sua nova senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha) {
}
