package br.com.certame.estudo;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.ModoNaoAlteravelException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.dto.RequisicaoFinalizacao;
import br.com.certame.estudo.dto.RequisicaoSessao;
import br.com.certame.estudo.dto.RespostaCorrecao;
import br.com.certame.estudo.dto.RespostaSessao;
import br.com.certame.estudo.dto.SessaoRetomavel;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessaoEstudoService {
    private final SessaoEstudoRepository sessoes;
    private final TempoSessao tempo;
    private final CorrecaoProvaService correcao;
    private final AbridorSessaoEstudo abridor;
    private final RetomadaSessaoEstudo retomada;
    private final MontadorRespostaSessao montadorResposta;
    private final Clock clock;
    public SessaoEstudoService(SessaoEstudoRepository sessoes, TempoSessao tempo, CorrecaoProvaService correcao,
            AbridorSessaoEstudo abridor, RetomadaSessaoEstudo retomada, MontadorRespostaSessao montadorResposta,
            Clock clock) {
        this.sessoes = sessoes;
        this.tempo = tempo;
        this.correcao = correcao;
        this.abridor = abridor;
        this.retomada = retomada;
        this.montadorResposta = montadorResposta;
        this.clock = clock;
    }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaSessao abrir(UUID usuarioId, RequisicaoSessao req) {
        return abridor.abrir(usuarioId, req);
    }
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaSessao abrir(UUID usuarioId, OrigemSessao origem, String origemRef, List<UUID> ids) {
        return abridor.abrir(usuarioId, origem, origemRef, ids);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public RespostaSessao retomar(UUID usuarioId, UUID sessaoId) {
        return retomada.retomar(buscar(usuarioId, sessaoId));
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Optional<SessaoRetomavel> retomavel(UUID usuarioId) {
        return retomada.retomavel(usuarioId);
    }
    /** Congela o cronômetro no servidor. Idempotente: sessão já pausada ou encerrada não é alterada. */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void pausar(UUID usuarioId, UUID sessaoId) {
        var sessao = buscar(usuarioId, sessaoId);
        if (sessao.getFinalizadaEm() != null || sessao.getExpiradaEm() != null || sessao.getPausadaEm() != null) {
            return;
        }
        var agora = clock.instant();
        sessao.setPausadaEm(agora);
        sessao.setUltimaAtividadeEm(agora);
    }
    /** Descongela: soma o intervalo pausado a pausadoMs e devolve a sessão retomada. */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public RespostaSessao retomarSessao(UUID usuarioId, UUID sessaoId) {
        var sessao = buscar(usuarioId, sessaoId);
        if (sessao.getPausadaEm() != null) {
            var agora = clock.instant();
            var intervaloPausadoMs = agora.toEpochMilli() - sessao.getPausadaEm().toEpochMilli();
            sessao.setPausadoMs(sessao.getPausadoMs() + (int) Math.min(Integer.MAX_VALUE, Math.max(0, intervaloPausadoMs)));
            sessao.setPausadaEm(null);
            sessao.setUltimaAtividadeEm(agora);
        }
        return retomada.retomar(sessao);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ModoSessao modoAtual(UUID usuarioId, UUID sessaoId) {
        return buscar(usuarioId, sessaoId).getModo();
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public RespostaSessao trocarModo(UUID usuarioId, UUID sessaoId, ModoSessao modo) {
        var sessao = buscar(usuarioId, sessaoId);
        if (sessao.getModo() == ModoSessao.PROVA || modo == null || modo == ModoSessao.PROVA) {
            throw new ModoNaoAlteravelException();
        }
        sessao.setModo(modo);
        return montadorResposta.respostaDaSessao(sessao);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public RespostaCorrecao finalizar(UUID usuarioId, UUID sessaoId, RequisicaoFinalizacao req) {
        var sessao = buscar(usuarioId, sessaoId);
        if (sessao.getFinalizadaEm() != null || sessao.getExpiradaEm() != null) {
            throw new SessaoEncerradaException();
        }
        if (sessao.getModo() != ModoSessao.PROVA && !req.respostasEfetivas().isEmpty()) {
            throw new DadosInvalidosException(Map.of("respostas", "Fora do Modo Prova, não envie respostas na finalização."));
        }
        if (req.pausadoMs() != null) {
            sessao.setPausadoMs(tempo.acumular(sessao.getPausadoMs(), req.pausadoMs()));
        }
        var lote = sessao.getModo() == ModoSessao.PROVA ? req.respostasEfetivas() : null;
        return correcao.corrigir(sessao, lote);
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public RespostaCorrecao revisar(UUID usuarioId, UUID sessaoId) {
        return correcao.revisar(buscar(usuarioId, sessaoId));
    }
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public SessaoEstudo sessaoParaVinculo(UUID usuarioId, UUID sessaoId) { return buscar(usuarioId, sessaoId); }
    private SessaoEstudo buscar(UUID usuarioId, UUID id) {
        return sessoes.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }
}
