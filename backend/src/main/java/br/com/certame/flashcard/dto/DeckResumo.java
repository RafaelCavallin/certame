package br.com.certame.flashcard.dto;

import java.util.UUID;

public record DeckResumo(UUID id, String nome, String descricao, long totalCards) {
}
