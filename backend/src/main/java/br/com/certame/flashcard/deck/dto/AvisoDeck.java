package br.com.certame.flashcard.deck.dto;

public record AvisoDeck(String tipo, String mensagem, long vencidos, long limiteDiario) {
    public static final String FILA_ACUMULADA = "FILA_ACUMULADA";
}
