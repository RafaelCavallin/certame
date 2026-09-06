package br.com.certame.simulado.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record RequisicaoSimulado(
        @NotBlank(message = "Informe o nome do simulado.") @Size(min = 3, max = 160, message = "O nome deve ter entre 3 e 160 caracteres.") String nome,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.") String descricao,
        UUID bancaId,
        @NotNull(message = "Informe o número de questões.") @Min(value = 10, message = "O número de questões deve estar entre 10 e 200.") @Max(value = 200, message = "O número de questões deve estar entre 10 e 200.") Short numeroQuestoes,
        @NotNull(message = "Informe a duração do simulado.") @Min(value = 600, message = "A duração deve estar entre 10 minutos e 6 horas.") @Max(value = 21600, message = "A duração deve estar entre 10 minutos e 6 horas.") Integer duracaoSegundos,
        Instant janelaAbertura, Instant janelaEncerramento) {}
