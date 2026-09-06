package br.com.certame.flashcard.fsrs;

import java.time.Instant;

/** Objeto parâmetro do agendador (`code-standards.md` §2.4): retenção e limites do deck, tempo e "agora". */
public record ContextoAgendamento(double retencaoAlvo, int intervaloMaxDias, int diasDecorridos, Instant agora) {
}
