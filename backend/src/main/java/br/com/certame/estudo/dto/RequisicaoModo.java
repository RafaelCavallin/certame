package br.com.certame.estudo.dto;

import br.com.certame.estudo.ModoSessao;
import jakarta.validation.constraints.NotNull;

public record RequisicaoModo(@NotNull(message = "Informe o modo da sessão.") ModoSessao modo) {}
