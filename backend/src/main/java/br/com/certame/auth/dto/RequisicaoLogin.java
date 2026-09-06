package br.com.certame.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoLogin(
        @NotBlank(message = "Informe seu e-mail.") @Email(message = "Informe um e-mail válido.") @Size(max = 255) String email,
        @NotBlank(message = "Informe sua senha.") @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String senha) {
}
