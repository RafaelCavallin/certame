package br.com.certame.flashcard.deck;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record LimitesDeck(short novosDia, short revisoesDia, short intervaloMaxDias, BigDecimal retencaoAlvo) {

    private static final short NOVOS_MIN = 0;
    private static final short NOVOS_MAX = 100;
    private static final short REVISOES_MIN = 5;
    private static final short REVISOES_MAX = 400;
    private static final short INTERVALO_MIN = 1;
    private static final short INTERVALO_MAX = 3650;
    private static final List<BigDecimal> RETENCOES_PERMITIDAS = List.of(
            new BigDecimal("0.80"), new BigDecimal("0.85"), new BigDecimal("0.90"), new BigDecimal("0.95"));

    public static final LimitesDeck PADRAO =
            new LimitesDeck((short) 10, (short) 60, (short) 180, new BigDecimal("0.90"));

    public static LimitesDeck de(Deck deck) {
        if (deck == null) {
            return PADRAO;
        }
        return new LimitesDeck(deck.getNovosDia(), deck.getRevisoesDia(), deck.getIntervaloMaxDias(), deck.getRetencaoAlvo());
    }

    public LimitesDeck normalizar() {
        return new LimitesDeck(
                clamp(novosDia, NOVOS_MIN, NOVOS_MAX),
                clamp(revisoesDia, REVISOES_MIN, REVISOES_MAX),
                clamp(intervaloMaxDias, INTERVALO_MIN, INTERVALO_MAX),
                retencaoMaisProxima(retencaoAlvo));
    }

    public String rotuloLimites() {
        return "%d novos/dia · %d revisões/dia · máx. %d d".formatted(novosDia, revisoesDia, intervaloMaxDias);
    }

    public String rotuloRetencao() {
        return "FSRS · retenção alvo %d%%".formatted(retencaoAlvo.multiply(new BigDecimal(100)).intValue());
    }

    private static short clamp(short valor, short min, short max) {
        return (short) Math.max(min, Math.min(max, valor));
    }

    private static BigDecimal retencaoMaisProxima(BigDecimal valor) {
        return RETENCOES_PERMITIDAS.stream()
                .min(Comparator.comparing(retencao -> retencao.subtract(valor).abs()))
                .orElseThrow();
    }
}
