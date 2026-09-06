package br.com.certame.simulado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RequisicaoComposicao(@NotNull(message = "Informe os itens da composição.") List<@Valid ItemComposicao> itens) {}
