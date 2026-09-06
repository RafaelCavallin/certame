package br.com.certame.flashcard.revisao.dto;

import br.com.certame.flashcard.revisao.EscopoFila;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoSessaoRevisao(
        @NotNull(message = "Informe o escopo da sessão.") EscopoFila escopo,
        UUID deckId,
        UUID disciplinaId) {
}
