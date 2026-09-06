package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.NotaRevisao;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.fsrs.AgendadorFsrs;
import br.com.certame.flashcard.fsrs.ContextoAgendamento;
import br.com.certame.flashcard.fsrs.CurvaEsquecimento;
import br.com.certame.flashcard.fsrs.EstadoMemoria;
import br.com.certame.flashcard.revisao.dto.CardRevisao;
import br.com.certame.flashcard.revisao.dto.MemoriaCard;
import br.com.certame.flashcard.revisao.dto.PrevisaoNota;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/** Constrói o CardRevisao com previsão e agendamento saindo da mesma chamada do agendador (CA-03). */
@Component
public class ConstrutorCardRevisao {
    private final AgendadorFsrs agendador = new AgendadorFsrs();
    private final RelogioEstudo relogio;

    public ConstrutorCardRevisao(RelogioEstudo relogio) {
        this.relogio = relogio;
    }

    public CardRevisao de(Flashcard card) {
        var agendamento = card.getAgendamento();
        var contexto = FabricaContextoAgendamento.de(card, relogio.instante());
        var memoriaEstado = new EstadoMemoria(agendamento.getEstabilidade(), agendamento.getDificuldade(),
                agendamento.getEstado(), agendamento.getPassoAprendizado());
        var previsoes = agendador.previsoes(memoriaEstado, contexto);
        var previsoesDto = Arrays.stream(NotaRevisao.values())
                .map(nota -> PrevisaoNota.de(nota, previsoes.minutos(nota))).toList();
        var memoria = MemoriaCard.de(agendamento.getEstabilidade(), agendamento.getDificuldade(),
                retencaoAtual(card, contexto));
        return CardRevisao.de(card, memoria, previsoesDto);
    }

    private static Double retencaoAtual(Flashcard card, ContextoAgendamento contexto) {
        var estabilidade = card.getAgendamento().getEstabilidade();
        if (estabilidade <= 0) {
            return null;
        }
        return CurvaEsquecimento.retrievability(estabilidade, contexto.diasDecorridos());
    }
}
