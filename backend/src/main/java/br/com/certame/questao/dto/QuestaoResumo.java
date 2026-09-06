package br.com.certame.questao.dto;

import br.com.certame.questao.StatusQuestao;
import br.com.certame.questao.TipoQuestao;
import java.time.Instant;
import java.util.UUID;

public record QuestaoResumo(UUID id, String codigo, String enunciadoResumo, String disciplina,
                            String banca, Short ano, TipoQuestao tipo, StatusQuestao status,
                            Instant atualizadoEm) {}
