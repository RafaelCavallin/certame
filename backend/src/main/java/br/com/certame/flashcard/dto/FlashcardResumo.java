package br.com.certame.flashcard.dto;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.StatusFlashcard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FlashcardResumo(UUID id, String frente, String verso, ReferenciaResumo deck,
        ReferenciaResumo disciplina, ReferenciaResumo assunto, List<String> tags,
        EstadoFlashcard estado, StatusFlashcard status, LocalDate proximaRevisao,
        OrigemResumo origem, Instant criadoEm) {

    public static FlashcardResumo de(Flashcard card) {
        var tags = card.getTags() == null ? List.<String>of() : List.of(card.getTags());
        return new FlashcardResumo(card.getId(), card.getFrente(), card.getVerso(), referenciaDeck(card),
                referenciaDisciplina(card), referenciaAssunto(card), tags, card.getAgendamento().getEstado(),
                card.getStatus(), card.getAgendamento().getProximaRevisao(), origem(card),
                card.getRegistroTemporal().getCriadoEm());
    }

    private static ReferenciaResumo referenciaDeck(Flashcard card) {
        var deck = card.getDeck();
        return deck == null ? null : new ReferenciaResumo(deck.getId(), deck.getNome());
    }

    private static ReferenciaResumo referenciaDisciplina(Flashcard card) {
        var disciplina = card.getOrigem().getDisciplina();
        return disciplina == null ? null : new ReferenciaResumo(disciplina.getId(), disciplina.getNome());
    }

    private static ReferenciaResumo referenciaAssunto(Flashcard card) {
        var assunto = card.getOrigem().getAssunto();
        return assunto == null ? null : new ReferenciaResumo(assunto.getId(), assunto.getNome());
    }

    private static OrigemResumo origem(Flashcard card) {
        var questao = card.getOrigem().getQuestaoOrigem();
        if (questao == null) return null;
        var sessao = card.getOrigem().getSessaoOrigem();
        return new OrigemResumo(questao.getId(), questao.getCodigo(), sessao == null ? null : sessao.getId());
    }
}
