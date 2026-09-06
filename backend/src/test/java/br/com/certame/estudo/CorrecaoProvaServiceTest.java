package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.certame.comum.evento.SessaoFinalizada;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.dto.*;
import br.com.certame.questao.*;
import br.com.certame.usuario.Usuario;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;

class CorrecaoProvaServiceTest {
    private static final UUID USUARIO_ID = id(1);
    private final SessaoQuestaoRepository sessoesQuestoes = mock(SessaoQuestaoRepository.class);
    private final RespostaRepository respostas = mock(RespostaRepository.class);
    private final ConferenciaGabarito conferencia = new ConferenciaGabarito();
    private final TempoSessao tempo = mock(TempoSessao.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    @Test void TU24_contaAcertosErrosENaoRespondidas() {
        var sessao = sessao(id(24)); var serie = serie(sessao, 30); var lote = lote(serie, 28, 22);
        var retorno = corrigir(sessao, serie, lote);
        assertThat(retorno.acertos()).isEqualTo(22);
        assertThat(retorno.erros()).isEqualTo(6);
        assertThat(retorno.naoRespondidas()).isEqualTo(2);
        assertThat(retorno.pctAcerto()).isEqualTo(73);
    }

    @Test void TU25_gravaOLoteInteiroEmUmaUnicaChamada() {
        var sessao = sessao(id(25)); var serie = serie(sessao, 30); var lote = lote(serie, 28, 28);
        corrigir(sessao, serie, lote);
        var captor = ArgumentCaptor.forClass(Iterable.class);
        verify(respostas).saveAll(captor.capture());
        assertThat(((Collection<?>) respostasSalvas(captor.getValue())).size()).isEqualTo(28);
    }

    @Test void TU26_montaRevisaoNaOrdemComItemEmBranco() {
        var sessao = sessao(id(26)); var serie = serie(sessao, 3);
        var retorno = corrigir(sessao, serie, List.of(new RespostaProva(serie.get(1).getQuestaoId(), "A", 10)));
        assertThat(retorno.itens()).extracting(ItemCorrecao::ordem).containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(retorno.itens().get(0).marcada()).isNull();
        assertThat(retorno.itens().get(0).correta()).isNull();
        assertThat(retorno.itens().get(1).marcada()).isEqualTo("A");
    }

    @Test void TU27_corrigeProvaSemNenhumaResposta() {
        var sessao = sessao(id(27)); var serie = serie(sessao, 10);
        var retorno = corrigir(sessao, serie, List.of());
        assertThat(retorno.pctAcerto()).isZero();
        assertThat(retorno.naoRespondidas()).isEqualTo(10);
        assertThat(retorno.itens()).allSatisfy(item -> assertThat(item.marcada()).isNull());
    }

    @Test void TU28_publicaSessaoFinalizadaUmaUnicaVez() {
        var sessao = sessao(id(28)); var serie = serie(sessao, 1);
        corrigir(sessao, serie, List.of());
        var captor = ArgumentCaptor.forClass(SessaoFinalizada.class);
        verify(eventos, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().sessaoId()).isEqualTo(sessao.getId());
        assertThat(captor.getValue().finalizadaEm()).isEqualTo(clock.instant());
    }

    @Test void TU57_recusaCorrigirSessaoJaFinalizada() {
        var sessao = sessao(id(29)); when(sessao.getFinalizadaEm()).thenReturn(clock.instant());
        assertThatThrownBy(() -> service().corrigir(sessao, List.of())).isInstanceOf(SessaoEncerradaException.class);
        verifyNoInteractions(respostas);
    }

    @Test void TU58_recusaCorrigirSessaoExpirada() {
        var sessao = sessao(id(30)); when(sessao.getExpiradaEm()).thenReturn(clock.instant());
        assertThatThrownBy(() -> service().corrigir(sessao, List.of())).isInstanceOf(SessaoEncerradaException.class);
    }

    @Test void TU59_recusaRevisarSessaoNaoFinalizada() {
        var sessao = sessao(id(31)); when(sessao.getFinalizadaEm()).thenReturn(null);
        assertThatThrownBy(() -> service().revisar(sessao)).isInstanceOf(SessaoEncerradaException.class);
    }

    @Test void TU60_recusaRevisarSessaoExpirada() {
        var sessao = sessao(id(32)); when(sessao.getFinalizadaEm()).thenReturn(clock.instant()); when(sessao.getExpiradaEm()).thenReturn(clock.instant());
        assertThatThrownBy(() -> service().revisar(sessao)).isInstanceOf(SessaoEncerradaException.class);
    }

    @Test void TU61_usaRespostasExistentesQuandoLoteNulo() {
        var sessao = sessao(id(33)); var serie = serie(sessao, 2);
        var resposta = respostaSalva(serie.get(0).getQuestaoId(), "A");
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie);
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of(resposta));
        when(tempo.duracaoSegundos(any(), any(), anyInt())).thenReturn(3600);

        var retorno = service().corrigir(sessao, null);

        assertThat(retorno.itens().get(0).marcada()).isEqualTo("A"); assertThat(retorno.naoRespondidas()).isEqualTo(1);
    }

    @Test void TU62_recusaLoteComQuestaoDesconhecida() {
        var sessao = sessao(id(34)); var serie = serie(sessao, 1);
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie);

        assertThatThrownBy(() -> service().corrigir(sessao, List.of(new RespostaProva(id(999), "A", 10))))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test void TU63_recusaLoteComQuestaoRepetida() {
        var sessao = sessao(id(35)); var serie = serie(sessao, 1);
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie);
        var questaoId = serie.get(0).getQuestaoId();

        assertThatThrownBy(() -> service().corrigir(sessao, List.of(new RespostaProva(questaoId, "A", 10), new RespostaProva(questaoId, "B", 10))))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test void TU64_naoGravaRespostasQuandoModoNaoEhProva() {
        var sessao = sessao(id(36)); when(sessao.getModo()).thenReturn(ModoSessao.FOCO); var serie = serie(sessao, 1);
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie);
        when(tempo.duracaoSegundos(any(), any(), anyInt())).thenReturn(3600);

        service().corrigir(sessao, List.of(new RespostaProva(serie.get(0).getQuestaoId(), "A", 10)));

        verify(respostas, never()).saveAll(any());
    }

    @Test void TU65_revisaMontandoCorrecaoAPartirDasRespostasSalvas() {
        var sessao = sessao(id(37)); when(sessao.getFinalizadaEm()).thenReturn(clock.instant()); var serie = serie(sessao, 2);
        var resposta = respostaSalva(serie.get(1).getQuestaoId(), "A");
        when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie);
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of(resposta));
        when(tempo.duracaoSegundos(any(), any(), anyInt())).thenReturn(1800);

        var retorno = service().revisar(sessao);

        assertThat(retorno.itens().get(1).marcada()).isEqualTo("A"); assertThat(retorno.acertos()).isEqualTo(1);
        verifyNoInteractions(eventos);
    }

    private Resposta respostaSalva(UUID questaoId, String marcada) { var r = mock(Resposta.class); var q = questao(questaoId); when(r.getQuestao()).thenReturn(q); when(r.getMarcada()).thenReturn(marcada); when(r.getTempoMs()).thenReturn(100); return r; }
    private RespostaCorrecao corrigir(SessaoEstudo sessao, List<SessaoQuestao> serie, List<RespostaProva> lote) { when(sessoesQuestoes.findBySessaoIdOrderByIdOrdem(sessao.getId())).thenReturn(serie); when(tempo.duracaoSegundos(any(), any(), anyInt())).thenReturn(3600); return service().corrigir(sessao, lote); }
    private CorrecaoProvaService service() { return new CorrecaoProvaService(sessoesQuestoes, respostas, conferencia, tempo, clock, eventos); }
    private SessaoEstudo sessao(UUID id) { var s = mock(SessaoEstudo.class); var u = mock(Usuario.class); when(u.getId()).thenReturn(USUARIO_ID); when(s.getId()).thenReturn(id); when(s.getUsuario()).thenReturn(u); when(s.getModo()).thenReturn(ModoSessao.PROVA); when(s.getFinalizadaEm()).thenReturn(null); when(s.getExpiradaEm()).thenReturn(null); when(s.getIniciadaEm()).thenReturn(clock.instant().minusSeconds(3600)); when(s.getPausadoMs()).thenReturn(0); return s; }
    private List<SessaoQuestao> serie(SessaoEstudo sessao, int total) { var lista = new ArrayList<SessaoQuestao>(); for (int i = 1; i <= total; i++) lista.add(new SessaoQuestao(sessao, (short) i, questao(id(100 + i)))); return lista; }
    private List<RespostaProva> lote(List<SessaoQuestao> serie, int quantidade, int acertos) { return java.util.stream.IntStream.range(0, quantidade).mapToObj(i -> new RespostaProva(serie.get(i).getQuestaoId(), i < acertos ? "A" : "B", 100)).toList(); }
    private Questao questao(UUID id) { var q = mock(Questao.class); when(q.getId()).thenReturn(id); when(q.getTipo()).thenReturn(TipoQuestao.ME); when(q.getGabarito()).thenReturn("A"); when(q.getCodigo()).thenReturn("Q"); when(q.getEnunciado()).thenReturn("Enunciado"); when(q.getComentario()).thenReturn(null); return q; }
    private Collection<?> respostasSalvas(Iterable<?> respostas) { var lista = new ArrayList<>(); respostas.forEach(lista::add); return lista; }
    private static UUID id(int n) { return UUID.nameUUIDFromBytes(("id-" + n).getBytes()); }
}
