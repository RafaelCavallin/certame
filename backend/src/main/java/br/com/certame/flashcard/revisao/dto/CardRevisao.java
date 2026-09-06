package br.com.certame.flashcard.revisao.dto;

import br.com.certame.flashcard.EstadoFlashcard;
import br.com.certame.flashcard.Flashcard;
import java.util.List;
import java.util.UUID;

/** Um card da fila, com tudo que a sessão precisa (RF-33). */
public record CardRevisao(UUID id, String frente, String verso, UUID deckId, String deckNome,
        String disciplinaNome, String assuntoNome, EstadoFlashcard estado, MemoriaCard memoria,
        List<PrevisaoNota> previsoes) {

    public static CardRevisao de(Flashcard card, MemoriaCard memoria, List<PrevisaoNota> previsoes) {
        var deck = card.getDeck();
        var disciplina = card.getOrigem().getDisciplina();
        var assunto = card.getOrigem().getAssunto();
        return new CardRevisao(card.getId(), card.getFrente(), card.getVerso(),
                deck == null ? null : deck.getId(), deck == null ? "Sem deck" : deck.getNome(),
                disciplina == null ? null : disciplina.getNome(), assunto == null ? null : assunto.getNome(),
                card.getAgendamento().getEstado(), memoria, previsoes);
    }
}
