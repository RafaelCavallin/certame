package br.com.certame.estudo;

import br.com.certame.comum.evento.SessaoFinalizada;
import br.com.certame.comum.excecao.*;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.Questao;
import java.time.Clock;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrecaoProvaService {
    private final SessaoQuestaoRepository sessoesQuestoes;
    private final RespostaRepository respostas;
    private final ConferenciaGabarito conferencia;
    private final TempoSessao tempo;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;
    public CorrecaoProvaService(SessaoQuestaoRepository sessoesQuestoes, RespostaRepository respostas, ConferenciaGabarito conferencia, TempoSessao tempo, Clock clock, ApplicationEventPublisher eventos) { this.sessoesQuestoes = sessoesQuestoes; this.respostas = respostas; this.conferencia = conferencia; this.tempo = tempo; this.clock = clock; this.eventos = eventos; }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaCorrecao corrigir(SessaoEstudo sessao, List<RespostaProva> lote) {
        if (sessao.getFinalizadaEm() != null || sessao.getExpiradaEm() != null) throw new SessaoEncerradaException();
        var serie = sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId());
        lote = lote == null ? respostasExistentes(sessao.getId()) : lote;
        var porQuestao = validarLote(sessao, serie, lote);
        if (sessao.getModo() == ModoSessao.PROVA) respostas.saveAll(porQuestao.values());
        var agora = clock.instant();
        sessao.setFinalizadaEm(agora);
        var resultado = montarCorrecao(sessao, serie, lote);
        eventos.publishEvent(new SessaoFinalizada(sessao.getUsuario().getId(), sessao.getId(), agora));
        return resultado;
    }
    @Transactional(readOnly = true)
    public RespostaCorrecao revisar(SessaoEstudo sessao) {
        if (sessao.getFinalizadaEm() == null || sessao.getExpiradaEm() != null) throw new SessaoEncerradaException();
        var serie = sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId());
        var lote = respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId()).stream().map(r -> new RespostaProva(r.getQuestao().getId(), r.getMarcada(), r.getTempoMs())).toList();
        return montarCorrecao(sessao, serie, lote);
    }
    private List<RespostaProva> respostasExistentes(UUID sessaoId) { return respostas.findBySessaoIdOrderByRespondidaEm(sessaoId).stream().map(r -> new RespostaProva(r.getQuestao().getId(), r.getMarcada(), r.getTempoMs())).toList(); }
    private Map<UUID, Resposta> validarLote(SessaoEstudo sessao, List<SessaoQuestao> serie, List<RespostaProva> lote) { var permitidas = serie.stream().collect(java.util.stream.Collectors.toMap(SessaoQuestao::getQuestaoId, i -> i)); var resultado = new LinkedHashMap<UUID, Resposta>(); for (var item : lote) { var sq = permitidas.get(item.questaoId()); if (sq == null) throw new RecursoNaoEncontradoException(); var q = sq.getQuestao(); var correta = conferencia.corrigir(q.getTipo(), q.getGabarito(), item.marcada()); if (resultado.put(item.questaoId(), new Resposta(sessao, sessao.getUsuario(), q, item.marcada(), correta, item.tempoMs())) != null) throw new DadosInvalidosException(Map.of("respostas", "Não repita questões na finalização.")); } return resultado; }
    private RespostaCorrecao montarCorrecao(SessaoEstudo sessao, List<SessaoQuestao> serie, List<RespostaProva> lote) { var dadas = lote.stream().collect(java.util.stream.Collectors.toMap(RespostaProva::questaoId, r -> r)); var acertos = 0; var erros = 0; var itens = new ArrayList<ItemCorrecao>(); for (var sq : serie) { var q = sq.getQuestao(); var dada = dadas.get(q.getId()); var correta = dada == null ? null : conferencia.corrigir(q.getTipo(), q.getGabarito(), dada.marcada()); if (Boolean.TRUE.equals(correta)) acertos++; if (Boolean.FALSE.equals(correta)) erros++; itens.add(new ItemCorrecao(sq.getId().getOrdem(), q.getId(), q.getCodigo(), q.getEnunciado(), q.getDisciplina() == null ? null : q.getDisciplina().getNome(), q.getGabarito(), dada == null ? null : dada.marcada(), correta, q.getComentario())); } var total = serie.size(); var pct = total == 0 ? 0 : (int) Math.round(100.0 * acertos / total); return new RespostaCorrecao(sessao.getId(), pct, acertos, erros, total - acertos - erros, total, tempo.duracaoSegundos(sessao.getIniciadaEm(), sessao.getFinalizadaEm(), sessao.getPausadoMs()), sessao.getFinalizadaEm(), itens); }
}
