package br.com.certame.estudo.caderno.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RequisicaoQuestoesCaderno(
        @NotEmpty(message = "Informe ao menos uma questão.")
        @Size(max = 200, message = "Informe no máximo 200 questões por lote.")
        List<@NotNull(message = "Informe um id de questão válido.") @Valid UUID> questaoIds) {}
