package br.com.certame.estudo.caderno.dto;

import br.com.certame.estudo.caderno.TipoCadernoAutomatico;
import java.time.Instant;
import java.util.UUID;

public record CadernoResumo(String id, String tipo, String nome, String descricao,
                            int totalQuestoes, int questoesRespondidas,
                            int progressoPercentual, Instant atualizadoEm, boolean editavel) {
    public static CadernoResumo manual(UUID id, String nome, String descricao, int total,
                                       int respondidas, int percentual, Instant atualizadoEm) {
        return new CadernoResumo(id.toString(), "MANUAL", nome, descricao, total, respondidas,
                percentual, atualizadoEm, true);
    }

    public static CadernoResumo automatico(TipoCadernoAutomatico tipo, String nome, String descricao,
                                           int total, int respondidas, int percentual, Instant atualizadoEm) {
        return new CadernoResumo("auto:" + tipo, "AUTOMATICO", nome, descricao, total, respondidas,
                percentual, atualizadoEm, false);
    }
}
