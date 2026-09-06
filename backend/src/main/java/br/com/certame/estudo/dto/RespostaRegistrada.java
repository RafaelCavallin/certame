package br.com.certame.estudo.dto;

import br.com.certame.questao.dto.EstatisticaQuestao;
import java.time.Instant;
import java.util.UUID;

public record RespostaRegistrada(UUID questaoId, String marcada, boolean correta, String gabarito,
                                 String comentario, EstatisticaQuestao estatistica, int tempoMs,
                                 Instant respondidaEm) {}
