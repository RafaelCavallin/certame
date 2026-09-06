package br.com.certame.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoCadastro(
        @NotBlank(message = "Informe seu nome.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.") String nome,
        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.") String email,
        @NotBlank(message = "Informe sua senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String senha) {
}
