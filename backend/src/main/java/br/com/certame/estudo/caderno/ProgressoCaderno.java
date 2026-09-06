package br.com.certame.estudo.caderno;

public final class ProgressoCaderno {
    private static final int PERCENTUAL = 100;

    private ProgressoCaderno() {}

    public static int percentual(int respondidas, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((float) respondidas * PERCENTUAL / total);
    }
}
