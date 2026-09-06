package br.com.certame.flashcard.revisao;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.FlashcardRepository;
import br.com.certame.flashcard.RelogioEstudo;
import br.com.certame.flashcard.StatusFlashcard;
import br.com.certame.flashcard.fsrs.Agendamento;
import br.com.certame.flashcard.fsrs.AgendadorFsrs;
import br.com.certame.flashcard.fsrs.EstadoMemoria;
import br.com.certame.flashcard.fsrs.PesosFsrs;
import br.com.certame.flashcard.revisao.dto.PrevisaoNota;
import br.com.certame.flashcard.revisao.dto.RequisicaoRevisao;
import br.com.certame.flashcard.revisao.dto.RespostaRevisao;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN-08: aplica o agendador, grava a linha imutável de log e devolve o novo agendamento (CA-07…CA-09). */
@Service
@PreAuthorize("isAuthenticated()")
public class RegistroRevisaoService {
    private final AgendadorFsrs agendador = new AgendadorFsrs();
    private final FlashcardRepository flashcards;
    private final FlashcardRevisaoRepository logs;
    private final ConstrutorCardRevisao construtorCard;
    private final LocalizadorSessaoRevisao localizadorSessao;
    private final RelogioEstudo relogio;

    public RegistroRevisaoService(FlashcardRepository flashcards, FlashcardRevisaoRepository logs,
            ConstrutorCardRevisao construtorCard, LocalizadorSessaoRevisao localizadorSessao, RelogioEstudo relogio) {
        this.flashcards = flashcards;
        this.logs = logs;
        this.construtorCard = construtorCard;
        this.localizadorSessao = localizadorSessao;
        this.relogio = relogio;
    }

    @Transactional
    public RespostaRevisao registrar(UUID sessaoId, UUID usuarioId, RequisicaoRevisao requisicao) {
        var sessao = localizadorSessao.aberta(sessaoId, usuarioId);
        var card = cardAtivo(requisicao.flashcardId(), usuarioId);
        var ctx = new ContextoRegistro(card, sessao, requisicao);
        var calculo = calcularAgendamento(ctx);
        aplicarAgendamento(card, calculo);
        registrarLog(ctx, calculo);
        return montarResposta(card, calculo.agendamento());
    }

    private Flashcard cardAtivo(UUID flashcardId, UUID usuarioId) {
        return flashcards.findByIdAndUsuarioIdAndStatus(flashcardId, usuarioId, StatusFlashcard.ATIVO)
                .orElseThrow(RecursoNaoEncontradoException::new);
    }

    private CalculoFsrs calcularAgendamento(ContextoRegistro ctx) {
        var card = ctx.card();
        var estadoFsrs = card.getAgendamento();
        var contexto = FabricaContextoAgendamento.de(card, relogio.instante());
        var memoriaAntes = new EstadoMemoria(estadoFsrs.getEstabilidade(), estadoFsrs.getDificuldade(),
                estadoFsrs.getEstado(), estadoFsrs.getPassoAprendizado());
        var agendamento = agendador.proximo(memoriaAntes, ctx.requisicao().nota(), contexto);
        return new CalculoFsrs(memoriaAntes, agendamento, contexto.diasDecorridos(), contexto.agora());
    }

    private void aplicarAgendamento(Flashcard card, CalculoFsrs calculo) {
        var agendamento = calculo.agendamento();
        var estadoFsrs = card.getAgendamento();
        estadoFsrs.setEstabilidade((float) agendamento.estabilidade());
        estadoFsrs.setDificuldade((float) agendamento.dificuldade());
        estadoFsrs.setEstado(agendamento.estado());
        estadoFsrs.setPassoAprendizado((short) agendamento.passoAprendizado());
        estadoFsrs.setUltimaRevisao(calculo.agora());
        card.agendarPara(agendamento.proximaRevisaoEm());
    }

    private void registrarLog(ContextoRegistro ctx, CalculoFsrs calculo) {
        var agendamento = calculo.agendamento();
        var antes = calculo.antes();
        var tempoMs = Math.min(ctx.requisicao().tempoMs(), PesosFsrs.TEMPO_MAX_POR_CARD_MS);
        var log = new FlashcardRevisao(ctx.card(), ctx.card().getUsuario(), ctx.sessao(), ctx.requisicao().nota().name(),
                (short) calculo.diasDecorridos(), (float) antes.estabilidade(), (float) agendamento.estabilidade(),
                (float) antes.dificuldade(), (float) agendamento.dificuldade(), (short) agendamento.intervaloDias(),
                agendamento.intervaloMinutos(), tempoMs, tempoMs / 1000);
        logs.save(log);
    }

    private RespostaRevisao montarResposta(Flashcard card, Agendamento agendamento) {
        var reenfileirar = agendamento.intervaloDias() == 0;
        var cardDto = reenfileirar ? construtorCard.de(card) : null;
        return new RespostaRevisao(card.getId(), card.getAgendamento().getEstado(), arredondar(agendamento.estabilidade()),
                arredondar(agendamento.dificuldade()), agendamento.intervaloMinutos(),
                PrevisaoNota.rotulo(agendamento.intervaloMinutos()), agendamento.proximaRevisaoEm(), reenfileirar, cardDto);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
