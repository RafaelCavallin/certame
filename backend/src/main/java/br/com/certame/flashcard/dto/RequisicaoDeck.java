package br.com.certame.flashcard.dto;

import java.math.BigDecimal;

public record RequisicaoDeck(
        String nome,
        String descricao,
        Short novosDia,
        Short revisoesDia,
        Short intervaloMaxDias,
        BigDecimal retencaoAlvo) {

    public RequisicaoDeck(String nome) {
        this(nome, null, null, null, null, null);
    }
}
