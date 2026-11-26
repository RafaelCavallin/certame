package br.com.certame.questao;

import java.time.Instant;

public record ProcedenciaQuestao(Fonte fonte, String identificadorExterno, String urlExterna,
        Instant importadoEm, Instant verificadoEm, String loteCodigo, boolean divergenciaPendente) {
}
