package br.com.certame.simulado;

import br.com.certame.estudo.*;
import br.com.certame.estudo.dto.*;
import br.com.certame.usuario.UsuarioRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TentativaSimuladoServiceTest {
    private final SimuladoRepository simulados = mock(SimuladoRepository.class);
    private final TentativaSimuladoRepository tentativas = mock(TentativaSimuladoRepository.class);
    private final SimuladoQuestaoRepository questoes = mock(SimuladoQuestaoRepository.class);
    private final SessaoEstudoService sessoes = mock(SessaoEstudoService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Simulado simulado = mock(Simulado.class);
    private final UUID usuarioId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC);
    private final TentativaSimuladoService service = new TentativaSimuladoService(simulados, tentativas, questoes, sessoes, usuarios, new JanelaSimulado(clock), clock);

    @Test void TU10_abreSessaoEmModoProvaComDuracaoDeclarada() {
        prepararSimulado();
        var sessao = new RespostaSessao(UUID.randomUUID(), ModoSessao.PROVA, OrigemSessao.SIMULADO, simulado.getId().toString(), 10, 0, Instant.EPOCH, 0, List.of(), List.of());
        when(sessoes.abrir(eq(usuarioId), eq(OrigemSessao.SIMULADO), anyString(), anyList())).thenReturn(sessao);
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(mock(br.com.certame.usuario.Usuario.class)));
        when(tentativas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.iniciar(usuarioId, simulado.getId());

        assertThat(resposta.duracaoSegundos()).isEqualTo(3600); verify(sessoes).abrir(eq(usuarioId), eq(OrigemSessao.SIMULADO), anyString(), anyList());
    }

    @Test void TU13_retomaTentativaEmAndamentoSemReiniciarCronometro() {
        prepararSimulado();
        var tentativa = mock(TentativaSimulado.class); var sessao = new RespostaSessao(UUID.randomUUID(), ModoSessao.PROVA, OrigemSessao.SIMULADO, simulado.getId().toString(), 10, 0, Instant.EPOCH, 100, List.of(), List.of());
        when(tentativa.getSimulado()).thenReturn(simulado); when(tentativa.getSessao()).thenReturn(mock(SessaoEstudo.class)); when(tentativa.getTipo()).thenReturn(TipoTentativa.VALIDA); when(tentativas.findBySimuladoIdAndUsuarioIdAndStatus(any(), any(), any())).thenReturn(Optional.of(tentativa)); when(sessoes.retomar(any(), any())).thenReturn(sessao);

        var resposta = service.iniciar(usuarioId, simulado.getId());

        assertThat(resposta.sessao().iniciadaEm()).isEqualTo(Instant.EPOCH); verify(sessoes, never()).abrir(any(), any(), any(), any());
    }

    @Test void TU11_marcaSegundaTentativaComoTreino() {
        prepararSimulado();
        var sessao = new RespostaSessao(UUID.randomUUID(), ModoSessao.PROVA, OrigemSessao.SIMULADO, simulado.getId().toString(), 10, 0, Instant.EPOCH, 0, List.of(), List.of());
        when(sessoes.abrir(eq(usuarioId), eq(OrigemSessao.SIMULADO), anyString(), anyList())).thenReturn(sessao);
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(mock(br.com.certame.usuario.Usuario.class)));
        when(tentativas.existsBySimuladoIdAndUsuarioIdAndTipoAndStatus(any(), any(), eq(TipoTentativa.VALIDA), eq(StatusTentativa.FINALIZADA))).thenReturn(true);
        when(tentativas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.iniciar(usuarioId, simulado.getId());

        assertThat(resposta.tipo()).isEqualTo(TipoTentativa.TREINO);
    }

    @Test void TU12_calculaNotaSobreOTotalDaProva() {
        var tentativa = tentativaEmAndamento(); var correcao = new RespostaCorrecao(UUID.randomUUID(), 80, 24, 5, 1, 30, 1200, Instant.EPOCH, List.of());
        when(sessoes.finalizar(any(), any(), any())).thenReturn(correcao); when(tentativas.findByIdAndUsuarioId(any(), any())).thenReturn(Optional.of(tentativa)); when(tentativas.save(any())).thenReturn(tentativa);

        var resultado = service.finalizar(usuarioId, UUID.randomUUID(), new RequisicaoFinalizacao(List.of(), 0));

        assertThat(resultado.nota()).isEqualTo(80); assertThat(resultado.total()).isEqualTo(30); assertThat(resultado.acertos() + resultado.erros() + resultado.naoRespondidas()).isEqualTo(30);
    }

    @Test void TU28_indicaTentativaEmAndamentoParaDecidirStatusHttp() {
        when(tentativas.findBySimuladoIdAndUsuarioIdAndStatus(any(), any(), eq(StatusTentativa.EM_ANDAMENTO))).thenReturn(Optional.of(mock(TentativaSimulado.class)));

        assertThat(service.temEmAndamento(usuarioId, UUID.randomUUID())).isTrue();

        when(tentativas.findBySimuladoIdAndUsuarioIdAndStatus(any(), any(), eq(StatusTentativa.EM_ANDAMENTO))).thenReturn(Optional.empty());

        assertThat(service.temEmAndamento(usuarioId, UUID.randomUUID())).isFalse();
    }

    private void prepararSimulado() { when(simulado.getId()).thenReturn(UUID.randomUUID()); when(simulado.getNome()).thenReturn("Prova"); when(simulado.getCodigo()).thenReturn("SM-001"); when(simulado.getStatus()).thenReturn(StatusSimulado.PUBLICADO); when(simulado.getJanelaAbertura()).thenReturn(Instant.EPOCH); when(simulado.getJanelaEncerramento()).thenReturn(Instant.parse("2026-09-03T00:00:00Z")); when(simulado.getDuracaoSegundos()).thenReturn(3600); when(simulados.findByIdAndExcluidoEmIsNull(any())).thenReturn(Optional.of(simulado)); when(tentativas.findBySimuladoIdAndUsuarioIdAndStatus(any(), any(), eq(StatusTentativa.EM_ANDAMENTO))).thenReturn(Optional.empty()); when(questoes.findBySimuladoIdOrderByIdOrdem(any())).thenReturn(List.of()); when(tentativas.existsBySimuladoIdAndUsuarioIdAndTipoAndStatus(any(), any(), any(), any())).thenReturn(false); }
    private TentativaSimulado tentativaEmAndamento() { prepararSimulado(); var tentativa = mock(TentativaSimulado.class); when(tentativa.getStatus()).thenReturn(StatusTentativa.EM_ANDAMENTO); when(tentativa.getSimulado()).thenReturn(simulado); when(tentativa.getTipo()).thenReturn(TipoTentativa.VALIDA); when(tentativa.getSessao()).thenReturn(mock(SessaoEstudo.class)); return tentativa; }
}
