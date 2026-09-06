package br.com.certame.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoItemCatalogo(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.") String nome,
        @Size(max = 20, message = "A sigla deve ter no máximo 20 caracteres.") String sigla) {}
