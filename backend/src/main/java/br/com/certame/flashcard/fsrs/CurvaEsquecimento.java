package br.com.certame.flashcard.fsrs;

/** Curva de esquecimento do FSRS-5 (RN-08): retenção em função do tempo e o intervalo inverso dela. */
public final class CurvaEsquecimento {

    private CurvaEsquecimento() {
    }

    public static double retrievability(double estabilidade, double dias) {
        if (estabilidade <= 0) {
            return 0;
        }
        return Math.pow(1 + PesosFsrs.FACTOR * dias / estabilidade, PesosFsrs.DECAY);
    }

    public static double intervaloParaRetencao(double estabilidade, double retencaoAlvo) {
        return estabilidade / PesosFsrs.FACTOR * (Math.pow(retencaoAlvo, 1 / PesosFsrs.DECAY) - 1);
    }
}
