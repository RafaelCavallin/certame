package br.com.certame.flashcard.revisao.dto;

import br.com.certame.flashcard.NotaRevisao;

/** O que cada classificação agendaria (CA-03) — o mesmo texto viaja na resposta do registro. */
public record PrevisaoNota(NotaRevisao nota, int minutos, String rotulo) {
    private static final int MINUTOS_POR_DIA = 1440;

    public static PrevisaoNota de(NotaRevisao nota, int minutos) {
        return new PrevisaoNota(nota, minutos, rotulo(minutos));
    }

    public static String rotulo(int minutos) {
        if (minutos % MINUTOS_POR_DIA != 0) {
            return minutos + " min";
        }
        var dias = minutos / MINUTOS_POR_DIA;
        return dias == 1 ? "1 dia" : dias + " dias";
    }
}
