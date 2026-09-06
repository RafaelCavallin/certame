package br.com.certame.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RequisicaoNoTaxonomia(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres.") String nome,
        UUID disciplinaId) {}
