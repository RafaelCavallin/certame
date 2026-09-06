package br.com.certame.questao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlternativaDto(
        String letra,
        @NotNull(message = "Informe o texto da alternativa.")
        @Size(min = 1, max = 2000, message = "O texto da alternativa deve ter no máximo 2000 caracteres.") String texto,
        @NotNull(message = "Informe a ordem da alternativa.")
        @Min(value = 1, message = "A ordem deve estar entre 1 e 5.")
        @Max(value = 5, message = "A ordem deve estar entre 1 e 5.") Short ordem) {
}
