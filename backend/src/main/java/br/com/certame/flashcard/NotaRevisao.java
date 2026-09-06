package br.com.certame.flashcard;

/** Classificação do estudante ao revisar um card (RN-08). */
public enum NotaRevisao {
    ERREI(1),
    DIFICIL(2),
    BOM(3),
    FACIL(4);

    private final int grau;

    NotaRevisao(int grau) {
        this.grau = grau;
    }

    public int grau() {
        return grau;
    }
}
