package br.com.certame.estudo.dto;

import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RespostaProva(@NotNull(message = "Informe a questão.") UUID questaoId,
                            @NotBlank(message = "Informe a alternativa marcada.") String marcada,
                            @Min(value = 0, message = "O tempo deve ser maior ou igual a zero.") int tempoMs) {}
