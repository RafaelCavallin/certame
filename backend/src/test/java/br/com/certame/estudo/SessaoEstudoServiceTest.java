package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import br.com.certame.comum.excecao.ModoNaoAlteravelException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SerieVaziaException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.caderno.ReferenciaCaderno;
import br.com.certame.estudo.caderno.SerieDoCadernoService;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.*;
import br.com.certame.usuario.Usuario;
import br.com.certame.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;

class SessaoEstudoServiceTest {
    private static final UUID USUARIO_ID = id(1);
    private final SessaoEstudoRepository sessoes = mock(SessaoEstudoRepository.class);
    private final SessaoQuestaoRepository questoes = mock(SessaoQuestaoRepository.class);
    private final RespostaRepository respostas = mock(RespostaRepository.class);
    private final MontadorSerie montador = mock(MontadorSerie.class);
    private final TempoSessao tempo = mock(TempoSessao.class);
    private final MarcacaoService marcacoes = mock(MarcacaoService.class);
    private final CorrecaoProvaService correcao = mock(CorrecaoProvaService.class);
    private final QuestaoRepository cadastroQuestoes = mock(QuestaoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SerieDoCadernoService serieDoCaderno = mock(SerieDoCadernoService.class);
    private final EstudoObservabilidade observabilidade = mock(EstudoObservabilidade.class);

    @Test void TU11_abreTodaSessaoEmFocoQuandoModoOmitido() {
        var id = id(11); var usuario = usuario(); var sessao = sessao(id(11), usuario, ModoSessao.FOCO);
        when(montador.montar(any(), any(), eq(USUARIO_ID))).thenReturn(List.of(id));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(sessoes.save(any())).thenReturn(sessao);
        var q = questao(id);
        when(cadastroQuestoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(q));
        quandoSerieVazia(sessao);
        var resposta = service().abrir(USUARIO_ID, new RequisicaoSessao(null, 10, null, null, null));
        assertThat(resposta.modo()).isEqualTo(ModoSessao.FOCO);
        assertThat(resposta.indiceAtual()).isZero();
        verify(questoes).saveAll(any());
    }

    @Test void TU12_trocaFocoParaContextoPreservandoIndiceERespostas() {
        var sessao = sessao(id(12), usuario(), ModoSessao.FOCO);
        var item = item(sessao, questao(id(121)), 1);
        when(sessao.getModo()).thenReturn(ModoSessao.FOCO, ModoSessao.CONTEXTO);
        var resposta = mock(Resposta.class);
        var respostaQuestao = item.getQuestao();
        when(resposta.getQuestao()).thenReturn(respostaQuestao);
        when(resposta.getMarcada()).thenReturn("B");
        when(resposta.getTempoMs()).thenReturn(1200);
        when(resposta.isCorreta()).thenReturn(true);
        when(resposta.getRespondidaEm()).thenReturn(Instant.parse("2026-08-31T10:00:00Z"));
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(questoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(List.of(item));
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of(resposta));
        quandoSerieVazia(sessao);
        var respostaSessao = service().trocarModo(USUARIO_ID, sessao.getId(), ModoSessao.CONTEXTO);
        assertThat(sessao.getModo()).isEqualTo(ModoSessao.CONTEXTO);
        assertThat(respostaSessao.indiceAtual()).isEqualTo(1);
        assertThat(respostaSessao.respostas()).hasSize(1);
        assertThat(respostaSessao.iniciadaEm()).isEqualTo(sessao.getIniciadaEm());
    }

    @Test void TU13_recusaTrocaDeModoEnvolvendoProva() {
        var sessao = sessao(id(13), usuario(), ModoSessao.FOCO);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        assertThatThrownBy(() -> service().trocarModo(USUARIO_ID, sessao.getId(), ModoSessao.PROVA))
                .isInstanceOf(ModoNaoAlteravelException.class);
        var prova = sessao(id(14), usuario(), ModoSessao.PROVA);
        when(sessoes.findByIdAndUsuarioId(prova.getId(), USUARIO_ID)).thenReturn(Optional.of(prova));
        assertThatThrownBy(() -> service().trocarModo(USUARIO_ID, prova.getId(), ModoSessao.FOCO))
                .isInstanceOf(ModoNaoAlteravelException.class);
    }

    @Test void TU14_recusaEscritaEmSessaoFinalizada() {
        var sessao = sessao(id(14), usuario(), ModoSessao.PROVA);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(sessao.getFinalizadaEm()).thenReturn(Instant.parse("2026-08-31T12:00:00Z"));
        assertThatThrownBy(() -> service().finalizar(USUARIO_ID, sessao.getId(), new RequisicaoFinalizacao(List.of(), null)))
                .isInstanceOf(SessaoEncerradaException.class);
        verifyNoInteractions(respostas);
    }

    @Test void TU15_retomaNaPrimeiraQuestaoSemResposta() {
        var sessao = sessao(id(15), usuario(), ModoSessao.FOCO);
        var itens = itens(sessao, 40, Set.of());
        var dadas = respostas(itens, 13);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(questoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(itens);
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(dadas);
        quandoSerieVazia(sessao);
        var retorno = service().retomar(USUARIO_ID, sessao.getId());
        assertThat(retorno.indiceAtual()).isEqualTo(13);
        assertThat(retorno.total()).isEqualTo(40);
    }

    @Test void TU16_retomaDescartandoQuestaoDespublicadaEReindexa() {
        var sessao = sessao(id(16), usuario(), ModoSessao.FOCO);
        var itens = itens(sessao, 30, Set.of(5, 17));
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(questoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(itens);
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of());
        quandoSerieVazia(sessao);
        var retorno = service().retomar(USUARIO_ID, sessao.getId());
        assertThat(retorno.total()).isEqualTo(28);
        assertThat(retorno.questoes()).extracting(QuestaoDaSerie::ordem)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 28).mapToObj(i -> (short) i).toList());
    }

    @Test void TU17_naoDevolveSessaoRetomavelDeOutroUsuario() {
        var sessaoId = id(17);
        when(sessoes.findByIdAndUsuarioId(sessaoId, USUARIO_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().retomar(USUARIO_ID, sessaoId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test void TU55_abreSessaoDeCadernoRestritaESemprEmFoco() {
        var cadernoId = id(18); var questaoId = id(180); var usuario = usuario(); var sessao = sessao(id(181), usuario, ModoSessao.FOCO);
        when(serieDoCaderno.questoes(eq(USUARIO_ID), any(ReferenciaCaderno.class))).thenReturn(List.of(questaoId));
        when(usuarios.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(sessoes.save(any())).thenReturn(sessao);
        var q = questao(questaoId);
        when(cadastroQuestoes.findByIdAndExcluidoEmIsNull(questaoId)).thenReturn(Optional.of(q));
        quandoSerieVazia(sessao);
        var resposta = service().abrir(USUARIO_ID, new RequisicaoSessao(ModoSessao.CONTEXTO, null, OrigemSessao.CADERNO, cadernoId.toString(), null));
        assertThat(resposta.modo()).isEqualTo(ModoSessao.FOCO);
        verifyNoInteractions(montador);
        verify(questoes).saveAll(any());
    }

    @Test void TU56_recusaSessaoDeCadernoSemQuestoes() {
        var cadernoId = id(19);
        when(serieDoCaderno.questoes(eq(USUARIO_ID), any(ReferenciaCaderno.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service().abrir(USUARIO_ID, new RequisicaoSessao(null, null, OrigemSessao.CADERNO, cadernoId.toString(), null)))
                .isInstanceOf(SerieVaziaException.class);
        verify(observabilidade).serieVazia(OrigemSessao.CADERNO);
    }

    @Test void TU60_pausarMarcaPausadaEm() {
        var sessao = sessao(id(60), usuario(), ModoSessao.FOCO);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        service().pausar(USUARIO_ID, sessao.getId());
        verify(sessao).setPausadaEm(Instant.parse("2026-08-31T12:00:00Z"));
    }

    @Test void TU61_pausarEhIdempotenteQuandoJaPausada() {
        var sessao = sessao(id(61), usuario(), ModoSessao.FOCO);
        when(sessao.getPausadaEm()).thenReturn(Instant.parse("2026-08-31T11:00:00Z"));
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        service().pausar(USUARIO_ID, sessao.getId());
        verify(sessao, never()).setPausadaEm(any());
    }

    @Test void TU62_pausarEmSessaoFinalizadaNaoAltera() {
        var sessao = sessao(id(62), usuario(), ModoSessao.FOCO);
        when(sessao.getFinalizadaEm()).thenReturn(Instant.parse("2026-08-31T11:00:00Z"));
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        service().pausar(USUARIO_ID, sessao.getId());
        verify(sessao, never()).setPausadaEm(any());
    }

    @Test void TU63_retomarSessaoAcumulaIntervaloPausadoEZeraPausadaEm() {
        var sessao = sessao(id(63), usuario(), ModoSessao.FOCO);
        when(sessao.getPausadaEm()).thenReturn(Instant.parse("2026-08-31T11:59:00Z"));
        when(sessao.getPausadoMs()).thenReturn(5_000);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(questoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(List.of());
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of());
        quandoSerieVazia(sessao);
        service().retomarSessao(USUARIO_ID, sessao.getId());
        verify(sessao).setPausadoMs(65_000);
        verify(sessao).setPausadaEm(null);
    }

    private SessaoEstudoService service() {
        var montadorResposta = new MontadorRespostaSessao(questoes, respostas, marcacoes, tempo);
        var abridor = new AbridorSessaoEstudo(sessoes, questoes, cadastroQuestoes, usuarios, montador, serieDoCaderno,
                observabilidade, montadorResposta);
        var retomada = new RetomadaSessaoEstudo(sessoes, montadorResposta, mock(TituloSessaoService.class));
        var clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC);
        return new SessaoEstudoService(sessoes, tempo, correcao, abridor, retomada, montadorResposta, clock);
    }
    private Usuario usuario() { var usuario = mock(Usuario.class); when(usuario.getId()).thenReturn(USUARIO_ID); return usuario; }
    private SessaoEstudo sessao(UUID id, Usuario usuario, ModoSessao modo) { var sessao = mock(SessaoEstudo.class); when(sessao.getId()).thenReturn(id); when(sessao.getUsuario()).thenReturn(usuario); when(sessao.getModo()).thenReturn(modo); when(sessao.getOrigem()).thenReturn(OrigemSessao.LIVRE); when(sessao.getIniciadaEm()).thenReturn(Instant.parse("2026-08-31T10:00:00Z")); when(sessao.getPausadoMs()).thenReturn(0); when(tempo.duracaoSegundos(any(), any(), anyInt())).thenReturn(42); when(tempo.duracaoSegundos(any(), any(), any(), anyInt())).thenReturn(42); return sessao; }
    private Questao questao(UUID id) { var questao = mock(Questao.class); when(questao.getId()).thenReturn(id); when(questao.getStatus()).thenReturn(StatusQuestao.PUBLICADA); when(questao.getTipo()).thenReturn(TipoQuestao.ME); when(questao.getCodigo()).thenReturn("Q-" + id); when(questao.getEnunciado()).thenReturn("Enunciado"); when(questao.getGabarito()).thenReturn("A"); when(questao.getAlternativas()).thenReturn(List.of()); return questao; }
    private SessaoQuestao item(SessaoEstudo sessao, Questao questao, int ordem) { return new SessaoQuestao(sessao, (short) ordem, questao); }
    private List<SessaoQuestao> itens(SessaoEstudo sessao, int total, Set<Integer> despublicadas) { return java.util.stream.IntStream.rangeClosed(1, total).mapToObj(i -> { var q = questao(id(1000 + i)); if (despublicadas.contains(i)) when(q.getStatus()).thenReturn(StatusQuestao.EM_REVISAO); return item(sessao, q, i); }).toList(); }
    private List<Resposta> respostas(List<SessaoQuestao> itens, int quantidade) { return itens.subList(0, quantidade).stream().map(item -> { var r = mock(Resposta.class); var questao = item.getQuestao(); when(r.getQuestao()).thenReturn(questao); when(r.getMarcada()).thenReturn("A"); when(r.getTempoMs()).thenReturn(1); when(r.isCorreta()).thenReturn(true); return r; }).toList(); }
    private void quandoSerieVazia(SessaoEstudo sessao) { when(marcacoes.marcadasDa(any(), any())).thenReturn(Map.of()); }
    private static UUID id(int valor) { return UUID.nameUUIDFromBytes(("id-" + valor).getBytes()); }
}
