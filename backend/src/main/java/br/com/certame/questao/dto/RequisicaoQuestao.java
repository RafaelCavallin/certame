package br.com.certame.questao.dto;

import br.com.certame.questao.Dificuldade;
import br.com.certame.questao.TipoQuestao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RequisicaoQuestao(
        @NotNull(message = "Informe o tipo da questão.") TipoQuestao tipo,
        @NotBlank(message = "Informe o enunciado.")
        @Size(min = 10, max = 20000, message = "O enunciado deve ter entre 10 e 20000 caracteres.") String enunciado,
        UUID disciplinaId,
        UUID assuntoId,
        UUID bancaId,
        UUID orgaoId,
        @Min(value = 1990, message = "O ano deve estar entre 1990 e 2100.")
        @Max(value = 2100, message = "O ano deve estar entre 1990 e 2100.") Short ano,
        Dificuldade dificuldade,
        @Valid List<AlternativaDto> alternativas,
        String gabarito,
        @Size(max = 20000, message = "O comentário deve ter no máximo 20000 caracteres.") String comentario) {
}
