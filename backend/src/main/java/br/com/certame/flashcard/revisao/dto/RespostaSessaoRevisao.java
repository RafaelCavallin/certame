package br.com.certame.flashcard.revisao.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespostaSessaoRevisao(UUID id, Instant iniciadaEm, String escopoRotulo, int total,
        List<CardRevisao> fila) {
}
