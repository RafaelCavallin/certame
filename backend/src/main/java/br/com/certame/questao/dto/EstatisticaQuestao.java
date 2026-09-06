package br.com.certame.questao.dto;

import java.time.Instant;
import java.util.UUID;

public record EstatisticaQuestao(
        UUID questaoId,
        int totalRespostas,
        int totalAcertos,
        Short pctAcerto,
        Integer tempoMedioMs,
        Instant atualizadoEm) {
}
