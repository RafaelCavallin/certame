package br.com.certame.flashcard.dto;

import java.util.UUID;

public record OrigemResumo(UUID questaoId, String codigo, UUID sessaoId) {
}
