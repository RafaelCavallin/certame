package br.com.certame.estudo.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

public record RequisicaoFinalizacao(@Valid List<RespostaProva> respostas,
                                    @Min(value = 0, message = "O tempo pausado deve ser maior ou igual a zero.") Integer pausadoMs) {
    public List<RespostaProva> respostasEfetivas() { return respostas == null ? List.of() : respostas; }
}
