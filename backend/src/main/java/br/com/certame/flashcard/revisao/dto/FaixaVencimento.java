package br.com.certame.flashcard.revisao.dto;

/** Uma linha de "Próximos vencimentos" — as três faixas de RF-34 são sempre as mesmas três. */
public record FaixaVencimento(String faixa, String rotulo, long quantidade) {
    public static final String AMANHA = "AMANHA";
    public static final String PROXIMOS_7_DIAS = "PROXIMOS_7_DIAS";
    public static final String DEPOIS_DE_7_DIAS = "DEPOIS_DE_7_DIAS";
}
