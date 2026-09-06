package br.com.certame.flashcard.revisao.dto;

import br.com.certame.flashcard.NotaRevisao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoRevisao(
        @NotNull(message = "Informe o card revisado.") UUID flashcardId,
        @NotNull(message = "Informe a nota da revisão.") NotaRevisao nota,
        @NotNull(message = "Informe o tempo gasto na revisão.")
        @Min(value = 0, message = "O tempo da revisão não pode ser negativo.") Integer tempoMs) {
}
