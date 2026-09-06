package br.com.certame.flashcard.fsrs;

import br.com.certame.flashcard.EstadoFlashcard;

/** Memória de um card antes da revisão — entrada pura do agendador (RN-08). */
public record EstadoMemoria(double estabilidade, double dificuldade, EstadoFlashcard estado, int passoAprendizado) {
}
