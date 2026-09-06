package br.com.certame.flashcard.revisao;

import br.com.certame.flashcard.Flashcard;
import br.com.certame.flashcard.fsrs.Agendamento;
import br.com.certame.flashcard.fsrs.EstadoMemoria;
import br.com.certame.flashcard.revisao.dto.RequisicaoRevisao;
import java.time.Instant;

/** Objetos parâmetro de `RegistroRevisaoService` — mantêm os métodos privados em três argumentos. */
record ContextoRegistro(Flashcard card, SessaoRevisao sessao, RequisicaoRevisao requisicao) {
}

record CalculoFsrs(EstadoMemoria antes, Agendamento agendamento, int diasDecorridos, Instant agora) {
}
