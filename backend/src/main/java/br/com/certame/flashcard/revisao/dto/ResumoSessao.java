package br.com.certame.flashcard.revisao.dto;

import java.util.List;

/** Corpo de `200` na finalização da sessão (RF-34, CA-10). */
public record ResumoSessao(int revisados, int classificacoes, Distribuicao distribuicao, int tempoMs,
        String tempoRotulo, List<FaixaVencimento> proximosVencimentos) {
    private static final int MS_POR_SEGUNDO = 1000;
    private static final int SEGUNDOS_POR_MINUTO = 60;

    public static String rotuloTempo(int tempoMs) {
        var totalSegundos = tempoMs / MS_POR_SEGUNDO;
        var minutos = totalSegundos / SEGUNDOS_POR_MINUTO;
        var segundos = totalSegundos % SEGUNDOS_POR_MINUTO;
        return "%d min %d s".formatted(minutos, segundos);
    }

    public record Distribuicao(int errei, int dificil, int bom, int facil) {
    }
}
