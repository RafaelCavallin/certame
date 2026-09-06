package br.com.certame.simulado;

import br.com.certame.comum.excecao.*;
import br.com.certame.estudo.*;
import br.com.certame.estudo.dto.*;
import br.com.certame.simulado.dto.*;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TentativaSimuladoService {
    private final SimuladoRepository simulados;
    private final TentativaSimuladoRepository tentativas;
    private final SimuladoQuestaoRepository questoes;
    private final SessaoEstudoService sessoes;
    private final UsuarioRepository usuarios;
    private final JanelaSimulado janela;
    private final Clock clock;

    public TentativaSimuladoService(SimuladoRepository simulados, TentativaSimuladoRepository tentativas, SimuladoQuestaoRepository questoes, SessaoEstudoService sessoes, UsuarioRepository usuarios, JanelaSimulado janela, Clock clock) {
        this.simulados = simulados; this.tentativas = tentativas; this.questoes = questoes; this.sessoes = sessoes; this.usuarios = usuarios; this.janela = janela; this.clock = clock;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public boolean temEmAndamento(UUID usuarioId, UUID simuladoId) {
        return tentativas.findBySimuladoIdAndUsuarioIdAndStatus(simuladoId, usuarioId, StatusTentativa.EM_ANDAMENTO).isPresent();
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaTentativa iniciar(UUID usuarioId, UUID simuladoId) {
        var simulado = obterSimulado(simuladoId);
        var aberta = tentativas.findBySimuladoIdAndUsuarioIdAndStatus(simuladoId, usuarioId, StatusTentativa.EM_ANDAMENTO);
        if (aberta.isPresent()) return resposta(aberta.get(), sessoes.retomar(usuarioId, aberta.get().getSessao().getId()));
        validarEntrada(simulado);
        var serie = questoes.findBySimuladoIdOrderByIdOrdem(simuladoId).stream().map(q -> q.getQuestao().getId()).toList();
        var sessao = sessoes.abrir(usuarioId, OrigemSessao.SIMULADO, simuladoId.toString(), serie);
        var usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        var tentativa = tentativas.save(new TentativaSimulado(simulado, usuario, sessoes.sessaoParaVinculo(usuarioId, sessao.sessaoId()), tipoPrevisto(simuladoId, usuarioId), clock.instant()));
        return resposta(tentativa, sessao);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaResultado finalizar(UUID usuarioId, UUID tentativaId, RequisicaoFinalizacao requisicao) {
        return finalizar(usuarioId, null, tentativaId, requisicao);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RespostaResultado finalizar(UUID usuarioId, UUID simuladoId, UUID tentativaId, RequisicaoFinalizacao requisicao) {
        var tentativa = obterTentativa(usuarioId, tentativaId);
        validarSimulado(simuladoId, tentativa);
        if (tentativa.getStatus() != StatusTentativa.EM_ANDAMENTO) throw new TentativaJaFinalizadaException();
        var correcao = sessoes.finalizar(usuarioId, tentativa.getSessao().getId(), requisicao);
        tentativa.finalizar((short) correcao.pctAcerto(), (short) correcao.acertos(), (short) correcao.erros(), (short) correcao.naoRespondidas(), correcao.tempoTotalSegundos(), clock.instant());
        tentativas.save(tentativa);
        var notaRegistrada = tentativa.getTipo() == TipoTentativa.TREINO ? notaValida(tentativa.getSimulado().getId(), usuarioId) : null;
        return resultado(tentativa, correcao, notaRegistrada);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public RespostaResultado resultado(UUID usuarioId, UUID tentativaId) {
        return resultado(usuarioId, null, tentativaId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public RespostaResultado resultado(UUID usuarioId, UUID simuladoId, UUID tentativaId) {
        var tentativa = obterTentativa(usuarioId, tentativaId);
        validarSimulado(simuladoId, tentativa);
        if (tentativa.getStatus() != StatusTentativa.FINALIZADA) throw new RecursoNaoEncontradoException("TENTATIVA_NAO_ENCONTRADA");
        var correcao = sessoes.revisar(usuarioId, tentativa.getSessao().getId());
        var notaRegistrada = tentativa.getTipo() == TipoTentativa.TREINO ? notaValida(tentativa.getSimulado().getId(), usuarioId) : null;
        return resultado(tentativa, correcao, notaRegistrada);
    }

    private Simulado obterSimulado(UUID id) { return simulados.findByIdAndExcluidoEmIsNull(id).orElseThrow(() -> new RecursoNaoEncontradoException("SIMULADO_NAO_ENCONTRADO")); }
    private TentativaSimulado obterTentativa(UUID usuarioId, UUID id) { return tentativas.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() -> new RecursoNaoEncontradoException("TENTATIVA_NAO_ENCONTRADA")); }
    private void validarSimulado(UUID simuladoId, TentativaSimulado tentativa) { if (simuladoId != null && !simuladoId.equals(tentativa.getSimulado().getId())) throw new RecursoNaoEncontradoException("TENTATIVA_NAO_ENCONTRADA"); }
    private void validarEntrada(Simulado simulado) { if (simulado.getStatus() != StatusSimulado.PUBLICADO) throw new SimuladoNaoPublicadoException(); if (!janela.aceitaNovaTentativa(janela.estado(simulado.getStatus(), simulado.getJanelaAbertura(), simulado.getJanelaEncerramento()))) throw new SimuladoForaDaJanelaException(); }
    private TipoTentativa tipoPrevisto(UUID simuladoId, UUID usuarioId) { return tentativas.existsBySimuladoIdAndUsuarioIdAndTipoAndStatus(simuladoId, usuarioId, TipoTentativa.VALIDA, StatusTentativa.FINALIZADA) ? TipoTentativa.TREINO : TipoTentativa.VALIDA; }
    private Integer notaValida(UUID simuladoId, UUID usuarioId) { return tentativas.findBySimuladoIdAndUsuarioIdAndTipoAndStatus(simuladoId, usuarioId, TipoTentativa.VALIDA, StatusTentativa.FINALIZADA).map(t -> (int) t.getNota()).orElse(null); }
    private RespostaTentativa resposta(TentativaSimulado tentativa, RespostaSessao sessao) { var s = tentativa.getSimulado(); return new RespostaTentativa(tentativa.getId(), s.getId(), s.getCodigo(), s.getNome(), tentativa.getTipo(), s.getDuracaoSegundos(), tentativa.getIniciadaEm(), sessao); }
    private RespostaResultado resultado(TentativaSimulado tentativa, RespostaCorrecao correcao, Integer notaRegistrada) { var s = tentativa.getSimulado(); return new RespostaResultado(tentativa.getId(), s.getId(), s.getCodigo(), s.getNome(), tentativa.getTipo(), correcao.pctAcerto(), correcao.acertos(), correcao.erros(), correcao.naoRespondidas(), correcao.total(), correcao.tempoTotalSegundos(), correcao.finalizadaEm(), notaRegistrada, correcao); }
}
