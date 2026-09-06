package br.com.certame.flashcard.fsrs;

/** Pesos padrão publicados do FSRS-5 (RN-08) — não trocar sem atualizar o PRD. */
public final class PesosFsrs {

    public static final double[] W = {
            0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192,
            1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621
    };

    public static final double DECAY = -0.5;
    public static final double FACTOR = 19.0 / 81.0;

    public static final double ESTABILIDADE_MIN = 0.01;
    public static final double ESTABILIDADE_MAX = 36500;

    public static final double DIFICULDADE_MIN = 1;
    public static final double DIFICULDADE_MAX = 10;

    public static final int[] PASSOS_APRENDIZADO_MINUTOS = { 1, 10 };
    public static final int[] PASSOS_REAPRENDIZADO_MINUTOS = { 10 };

    public static final int TEMPO_MAX_POR_CARD_MS = 600_000;

    private PesosFsrs() {
    }
}
