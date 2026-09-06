package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.deck.LimitesDeck;
import br.com.certame.flashcard.fsrs.ContextoAgendamento;
import java.time.Duration;
import java.time.Instant;

/** Monta o objeto parâmetro do agendador a partir do card e do relógio (RN-08). */
final class FabricaContextoAgendamento {

    private FabricaContextoAgendamento() {
    }

    static ContextoAgendamento de(Flashcard card, Instant agora) {
        var limites = LimitesDeck.de(card.getDeck());
        return new ContextoAgendamento(limites.retencaoAlvo().doubleValue(), limites.intervaloMaxDias(),
                diasDecorridos(card, agora), agora);
    }

    private static int diasDecorridos(Flashcard card, Instant agora) {
        var ultimaRevisao = card.getAgendamento().getUltimaRevisao();
        if (ultimaRevisao == null) {
            return 0;
        }
        return (int) Duration.between(ultimaRevisao, agora).toDays();
    }
}
