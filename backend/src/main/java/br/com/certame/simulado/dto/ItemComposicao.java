package br.com.certame.simulado.dto;

import br.com.certame.questao.Dificuldade;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ItemComposicao(@NotNull(message = "Informe a disciplina.") UUID disciplinaId,
                             Dificuldade dificuldade,
                             @NotNull(message = "Informe a quantidade.") @Min(value = 1, message = "A quantidade deve ser no mínimo 1.") Short quantidade) {}
