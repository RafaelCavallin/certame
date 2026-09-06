package br.com.certame.flashcard.dto;

import java.util.List;
import java.util.UUID;

public record RequisicaoMoverCards(List<UUID> flashcardIds) {
}
