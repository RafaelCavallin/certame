package br.com.certame.estudo.inicio;

import br.com.certame.comum.ZonaAplicacao;
import br.com.certame.estudo.*;
import br.com.certame.estudo.dto.ResumoInicio;
import java.time.*;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumoDiaService {
    private static final int MILIS_POR_SEGUNDO = 1000;
    private final SessaoEstudoRepository sessoes;
    private final RespostaRepository respostas;
    private final TempoSessao tempo;
    private final TempoEstudoExterno tempoExterno;
    private final Clock clock;
    private final TituloSessaoService titulos;
    // @Nullable é proposital: mantém esta entrega independente de prd-flashcards-decks-e-revisao para compilar.
    public ResumoDiaService(SessaoEstudoRepository sessoes, RespostaRepository respostas, TempoSessao tempo,
                            @Nullable TempoEstudoExterno tempoExterno, Clock clock, TituloSessaoService titulos) {
        this.sessoes = sessoes;
        this.respostas = respostas;
        this.tempo = tempo;
        this.tempoExterno = tempoExterno;
        this.clock = clock;
        this.titulos = titulos;
    }
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResumoInicio doDia(UUID usuarioId, LocalDate dia) {
        var data = dia == null ? LocalDate.now(clock.withZone(ZonaAplicacao.AMERICA_SAO_PAULO)) : dia;
        var inicio = data.atStartOfDay(ZonaAplicacao.AMERICA_SAO_PAULO).toInstant();
        var fim = data.plusDays(1).atStartOfDay(ZonaAplicacao.AMERICA_SAO_PAULO).toInstant();
        var respostasDoDia = respostas.findByUsuarioIdAndRespondidaEmGreaterThanEqualAndRespondidaEmLessThan(usuarioId, inicio, fim);
        var questoes = tempoQuestoes(usuarioId, inicio, fim);
        var flashcards = tempoExterno == null ? 0 : Math.max(0, tempoExterno.segundosNoDia(usuarioId, data));
        return new ResumoInicio(data, respostasDoDia.size(), percentual(respostasDoDia), questoes + flashcards, questoes, flashcards,
                sessoes.findByUsuarioIdAndModoNotAndFinalizadaEmIsNullAndExpiradaEmIsNullOrderByUltimaAtividadeEmDesc(usuarioId, ModoSessao.PROVA).stream()
                        .map(sessao -> retomavel(sessao, usuarioId)).toList());
    }
    private int tempoQuestoes(UUID usuarioId, Instant inicio, Instant fim) {
        return sessoes.findByUsuarioIdAndIniciadaEmGreaterThanEqualAndIniciadaEmLessThan(usuarioId, inicio, fim).stream()
                .mapToInt(sessao -> tempo.duracaoSegundos(sessao.getIniciadaEm(), sessao.getFinalizadaEm(), sessao.getPausadaEm(), sessao.getPausadoMs())).sum();
    }
    private Integer percentual(java.util.List<Resposta> itens) {
        if (itens.isEmpty()) return null;
        return (int) Math.round(itens.stream().filter(Resposta::isCorreta).count() * 100.0 / itens.size());
    }
    private br.com.certame.estudo.dto.SessaoRetomavel retomavel(SessaoEstudo sessao, UUID usuarioId) {
        var titulo = titulos.titulo(sessao.getId(), sessao.getOrigem(), sessao.getOrigemRef(), usuarioId);
        return new br.com.certame.estudo.dto.SessaoRetomavel(sessao.getId(), sessao.getModo(), sessao.getOrigem(),
                titulo, respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId()).size(), sessao.getTamanho(),
                sessao.getUltimaAtividadeEm());
    }
}
