package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.NotaRevisao;

/** Estabilidade do FSRS-5 (RN-08): primeiro contato, acerto, lapso e a fórmula de mesmo dia. */
public final class CalculoEstabilidade {

    private CalculoEstabilidade() {
    }

    public static double inicial(NotaRevisao nota) {
        return saturar(PesosFsrs.W[nota.grau() - 1]);
    }

    public static double aposAcerto(double estabilidade, double dificuldade, double retrievability,
            NotaRevisao nota) {
        var penalidadeDificil = nota == NotaRevisao.DIFICIL ? PesosFsrs.W[15] : 1;
        var bonusFacil = nota == NotaRevisao.FACIL ? PesosFsrs.W[16] : 1;
        var fator = Math.exp(PesosFsrs.W[8]) * (11 - dificuldade) * Math.pow(estabilidade, -PesosFsrs.W[9])
                * (Math.exp(PesosFsrs.W[10] * (1 - retrievability)) - 1) * penalidadeDificil * bonusFacil;
        return saturar(estabilidade * (1 + fator));
    }

    public static double aposLapso(double estabilidade, double dificuldade, double retrievability) {
        var lapso = PesosFsrs.W[11] * Math.pow(dificuldade, -PesosFsrs.W[12])
                * (Math.pow(estabilidade + 1, PesosFsrs.W[13]) - 1)
                * Math.exp(PesosFsrs.W[14] * (1 - retrievability));
        return saturar(Math.min(estabilidade, lapso));
    }

    public static double curtoPrazo(double estabilidade, NotaRevisao nota) {
        return saturar(estabilidade * Math.exp(PesosFsrs.W[17] * (nota.grau() - 3 + PesosFsrs.W[18])));
    }

    private static double saturar(double estabilidade) {
        return Math.min(PesosFsrs.ESTABILIDADE_MAX, Math.max(PesosFsrs.ESTABILIDADE_MIN, estabilidade));
    }
}
