package br.com.certame.flashcard.revisao.dto;

import java.util.Locale;

/** Bloco de memória exibido no verso do card (CA-04). */
public record MemoriaCard(double estabilidade, double dificuldade, Double retencaoAtual, String rotulo) {
    private static final Locale PT_BR = Locale.of("pt", "BR");

    public static MemoriaCard de(double estabilidade, double dificuldade, Double retencaoAtual) {
        var estabilidade2casas = arredondar(estabilidade);
        var dificuldade2casas = arredondar(dificuldade);
        var retencao2casas = retencaoAtual == null ? null : arredondar(retencaoAtual);
        return new MemoriaCard(estabilidade2casas, dificuldade2casas, retencao2casas,
                rotulo(estabilidade2casas, dificuldade2casas, retencao2casas));
    }

    private static String rotulo(double estabilidade, double dificuldade, Double retencaoAtual) {
        if (retencaoAtual == null) {
            return "Primeiro contato com este card";
        }
        var dias = Math.round(estabilidade);
        var retencaoPct = Math.round(retencaoAtual * 100);
        return "Estabilidade %d dias · dificuldade %s de 10 · retenção hoje %d%%"
                .formatted(dias, String.format(PT_BR, "%.1f", dificuldade), retencaoPct);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
