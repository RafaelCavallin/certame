package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.RespostaDuplicadaException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.*;
import br.com.certame.questao.dto.EstatisticaQuestao;
import br.com.certame.usuario.Usuario;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistroRespostaServiceTest {
    private static final UUID USUARIO_ID = UUID.nameUUIDFromBytes("usuario".getBytes());
    private final SessaoEstudoRepository sessoes = mock(SessaoEstudoRepository.class);
    private final SessaoQuestaoRepository sessoesQuestoes = mock(SessaoQuestaoRepository.class);
    private final RespostaRepository respostas = mock(RespostaRepository.class);
    private final EstatisticaQuestaoService estatistica = mock(EstatisticaQuestaoService.class);
    private final ConferenciaGabarito conferencia = new ConferenciaGabarito();
    private final TempoSessao tempo = new TempoSessao(Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC));
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC);

    @Test void TU18_gravaQuestaoMarcadaAcertoTempoEOrigem() {
        var q = questao(id(18), "B", "Comentário"); var sessao = sessao(id(18), ModoSessao.FOCO, OrigemSessao.CADERNO);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        var item = item(sessao, q);
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(List.of(item));
        when(respostas.findBySessaoIdAndQuestaoId(sessao.getId(), q.getId())).thenReturn(Optional.empty());
        when(respostas.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(estatistica.recalcular(q.getId())).thenReturn(null);
        var retorno = service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "B", 2300, null));
        var captor = ArgumentCaptor.forClass(Resposta.class);
        verify(respostas).saveAndFlush(captor.capture());
        var salva = captor.getValue();
        assertThat(salva.getQuestao()).isSameAs(q);
        assertThat(salva.getMarcada()).isEqualTo("B");
        assertThat(salva.isCorreta()).isTrue();
        assertThat(salva.getTempoMs()).isEqualTo(2300);
        assertThat(salva.getSessao().getOrigem()).isEqualTo(OrigemSessao.CADERNO);
        assertThat(retorno.questaoId()).isEqualTo(q.getId());
    }

    @Test void TU19_devolveGabaritoComentarioEEstatistica() {
        var q = questao(id(19), "A", "Comentário"); var sessao = sessao(id(19), ModoSessao.FOCO, OrigemSessao.LIVRE);
        var agregado = mock(EstatisticaQuestao.class);
        preparar(sessao, q); when(estatistica.recalcular(q.getId())).thenReturn(agregado);
        var retorno = service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "A", 100, null));
        assertThat(retorno.gabarito()).isEqualTo("A");
        assertThat(retorno.comentario()).isEqualTo("Comentário");
        assertThat(retorno.estatistica()).isSameAs(agregado);
        verify(estatistica).recalcular(q.getId());
    }

    @Test void TU20_devolveEstatisticaNulaQuandoSoAutorRespondeu() {
        var q = questao(id(20), "A", null); var sessao = sessao(id(20), ModoSessao.FOCO, OrigemSessao.LIVRE);
        preparar(sessao, q); when(estatistica.recalcular(q.getId())).thenReturn(null);
        var retorno = service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "A", 100, null));
        assertThat(retorno.estatistica()).isNull();
    }

    @Test void TU21_recusaSegundaRespostaDaMesmaQuestao() {
        var q = questao(id(21), "A", null); var sessao = sessao(id(21), ModoSessao.FOCO, OrigemSessao.LIVRE);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(respostas.findBySessaoIdAndQuestaoId(sessao.getId(), q.getId())).thenReturn(Optional.of(mock(Resposta.class)));
        assertThatThrownBy(() -> service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "A", 1, null)))
                .isInstanceOf(RespostaDuplicadaException.class);
        verify(respostas, never()).saveAndFlush(any());
    }

    @Test void TU22_recusaRespostaEmSessaoDeModoProva() {
        var q = questao(id(22), "A", null); var sessao = sessao(id(22), ModoSessao.PROVA, OrigemSessao.LIVRE);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        assertThatThrownBy(() -> service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "A", 1, null)))
                .isInstanceOf(SessaoEncerradaException.class)
                .hasMessage("A sessão de estudo já foi encerrada.");
        verify(respostas, never()).saveAndFlush(any());
    }

    @Test void TU23_recusaQuestaoForaDaSerie() {
        var q = questao(id(23), "A", null); var outra = questao(id(230), "A", null); var sessao = sessao(id(23), ModoSessao.FOCO, OrigemSessao.LIVRE);
        when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao));
        when(respostas.findBySessaoIdAndQuestaoId(sessao.getId(), q.getId())).thenReturn(Optional.empty());
        var item = item(sessao, outra);
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(List.of(item));
        assertThatThrownBy(() -> service().registrar(USUARIO_ID, sessao.getId(), new RequisicaoResposta(q.getId(), "A", 1, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(respostas, never()).saveAndFlush(any());
    }

    private RegistroRespostaService service() { return new RegistroRespostaService(sessoes, sessoesQuestoes, respostas, estatistica, conferencia, tempo, clock); }
    private void preparar(SessaoEstudo sessao, Questao q) { var item = item(sessao, q); when(sessoes.findByIdAndUsuarioId(sessao.getId(), USUARIO_ID)).thenReturn(Optional.of(sessao)); when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(List.of(item)); when(respostas.findBySessaoIdAndQuestaoId(sessao.getId(), q.getId())).thenReturn(Optional.empty()); when(respostas.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0)); }
    private SessaoEstudo sessao(UUID id, ModoSessao modo, OrigemSessao origem) { var s = mock(SessaoEstudo.class); var u = mock(Usuario.class); when(u.getId()).thenReturn(USUARIO_ID); when(s.getId()).thenReturn(id); when(s.getUsuario()).thenReturn(u); when(s.getModo()).thenReturn(modo); when(s.getOrigem()).thenReturn(origem); when(s.getPausadoMs()).thenReturn(0); when(s.getFinalizadaEm()).thenReturn(null); when(s.getExpiradaEm()).thenReturn(null); return s; }
    private Questao questao(UUID id, String gabarito, String comentario) { var q = mock(Questao.class); when(q.getId()).thenReturn(id); when(q.getTipo()).thenReturn(TipoQuestao.ME); when(q.getGabarito()).thenReturn(gabarito); when(q.getComentario()).thenReturn(comentario); return q; }
    private SessaoQuestao item(SessaoEstudo sessao, Questao q) { return new SessaoQuestao(sessao, (short) 1, q); }
    private static UUID id(int n) { return UUID.nameUUIDFromBytes(("id-" + n).getBytes()); }
}
