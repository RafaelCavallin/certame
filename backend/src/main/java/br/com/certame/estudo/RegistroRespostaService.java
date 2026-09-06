package br.com.certame.estudo;

import br.com.certame.comum.excecao.*;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.Questao;
import br.com.certame.questao.EstatisticaQuestaoService;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroRespostaService {
    private final SessaoEstudoRepository sessoes;
    private final SessaoQuestaoRepository sessoesQuestao;
    private final RespostaRepository respostas;
    private final EstatisticaQuestaoService estatistica;
    private final ConferenciaGabarito conferencia;
    private final TempoSessao tempo;
    private final Clock clock;
    public RegistroRespostaService(SessaoEstudoRepository sessoes, SessaoQuestaoRepository sessoesQuestao, RespostaRepository respostas, EstatisticaQuestaoService estatistica, ConferenciaGabarito conferencia, TempoSessao tempo, Clock clock) { this.sessoes = sessoes; this.sessoesQuestao = sessoesQuestao; this.respostas = respostas; this.estatistica = estatistica; this.conferencia = conferencia; this.tempo = tempo; this.clock = clock; }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaRegistrada registrar(UUID usuarioId, UUID sessaoId, RequisicaoResposta req) {
        var sessao = sessoes.findByIdAndUsuarioId(sessaoId, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        validarSessao(sessao);
        if (req.tempoMs() < 0 || req.pausadoMs() != null && req.pausadoMs() < 0) throw new DadosInvalidosException(Map.of("tempoMs", "O tempo deve ser maior ou igual a zero."));
        var item = respostas.findBySessaoIdAndQuestaoId(sessaoId, req.questaoId());
        if (item.isPresent()) throw new RespostaDuplicadaException();
        var questao = questaoDaSerie(sessaoId, req.questaoId());
        var correta = conferencia.corrigir(questao.getTipo(), questao.getGabarito(), req.marcada());
        var salva = respostas.saveAndFlush(new Resposta(sessao, sessao.getUsuario(), questao, req.marcada(), correta, req.tempoMs()));
        atualizarTempo(sessao, req.pausadoMs());
        var agregado = estatistica.recalcular(questao.getId());
        return new RespostaRegistrada(questao.getId(), salva.getMarcada(), correta, questao.getGabarito(), questao.getComentario(), agregado, salva.getTempoMs(), salva.getRespondidaEm());
    }
    private void validarSessao(SessaoEstudo sessao) { if (sessao.getFinalizadaEm() != null || sessao.getExpiradaEm() != null || sessao.getModo() == ModoSessao.PROVA) throw new SessaoEncerradaException(); }
    private Questao questaoDaSerie(UUID sessaoId, UUID questaoId) { return questoes(sessaoId).stream().filter(i -> i.getQuestaoId().equals(questaoId)).map(SessaoQuestao::getQuestao).findFirst().orElseThrow(RecursoNaoEncontradoException::new); }
    private List<SessaoQuestao> questoes(UUID sessaoId) { return sessoesQuestao.findBySessaoIdOrderByIdOrdem(sessaoId); }
    private void atualizarTempo(SessaoEstudo sessao, Integer pausadoMs) { sessao.setUltimaAtividadeEm(clock.instant()); if (pausadoMs != null) sessao.setPausadoMs(tempo.acumular(sessao.getPausadoMs(), pausadoMs)); }
}
