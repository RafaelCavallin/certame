package br.com.certame.estudo.caderno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoCaderno(
        @NotBlank(message = "Informe o nome do caderno.")
        @Size(min = 3, max = 80, message = "O nome deve ter entre 3 e 80 caracteres.") String nome,
        @Size(max = 240, message = "A descrição deve ter no máximo 240 caracteres.") String descricao) {}
