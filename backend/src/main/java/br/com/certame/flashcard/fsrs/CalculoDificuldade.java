package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.NotaRevisao;

/** Dificuldade do FSRS-5 (RN-08): valor inicial por nota e reversão à média a cada revisão. */
public final class CalculoDificuldade {

    private CalculoDificuldade() {
    }

    public static double inicial(NotaRevisao nota) {
        return d0(nota);
    }

    public static double proxima(double dificuldadeAtual, NotaRevisao nota) {
        var revertida = clamp(dificuldadeAtual - PesosFsrs.W[6] * (nota.grau() - 3) * (10 - dificuldadeAtual) / 9);
        return clamp(PesosFsrs.W[7] * d0(NotaRevisao.FACIL) + (1 - PesosFsrs.W[7]) * revertida);
    }

    private static double d0(NotaRevisao nota) {
        return clamp(PesosFsrs.W[4] - Math.exp(PesosFsrs.W[5] * (nota.grau() - 1)) + 1);
    }

    private static double clamp(double dificuldade) {
        return Math.min(PesosFsrs.DIFICULDADE_MAX, Math.max(PesosFsrs.DIFICULDADE_MIN, dificuldade));
    }
}
