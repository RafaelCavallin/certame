package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.NotaRevisao;
import java.util.Map;

/** Saída de `AgendadorFsrs.previsoes` (CA-03): o intervalo que cada nota agendaria. */
public record PrevisaoIntervalos(Map<NotaRevisao, Integer> minutosPorNota) {

    public int minutos(NotaRevisao nota) {
        return minutosPorNota.get(nota);
    }
}
