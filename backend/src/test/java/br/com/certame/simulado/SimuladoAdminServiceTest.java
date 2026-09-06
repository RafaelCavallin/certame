package br.com.certame.simulado;

import br.com.certame.catalogo.*;
import br.com.certame.comum.Paginacao;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ComposicaoInvalidaException;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.simulado.dto.*;
import br.com.certame.questao.QuestaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SimuladoAdminServiceTest {
    private final SimuladoRepository simulados = mock(SimuladoRepository.class);
    private final SimuladoComposicaoRepository composicoes = mock(SimuladoComposicaoRepository.class);
    private final TentativaSimuladoRepository tentativas = mock(TentativaSimuladoRepository.class);
    private final DisciplinaRepository disciplinas = mock(DisciplinaRepository.class);
    private final BancaRepository bancas = mock(BancaRepository.class);
    private final QuestaoRepository questoes = mock(QuestaoRepository.class);
    private final RegistradorAuditoria auditoria = mock(RegistradorAuditoria.class);
    private final ConsultaSimuladoAdmin consulta =
            new ConsultaSimuladoAdmin(composicoes, tentativas, questoes, new JanelaSimulado(Clock.systemUTC()));
    private final ComposicaoSimuladoService composicaoService =
            new ComposicaoSimuladoService(composicoes, disciplinas, auditoria, consulta);
    private final LocalizadorSimulado localizador = new LocalizadorSimulado(simulados, tentativas);
    private final SimuladoAdminService service = new SimuladoAdminService(simulados, mock(GeradorCodigoSimulado.class),
            auditoria, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), consulta, new MontadorSimulado(bancas),
            composicaoService, localizador);

    @Test void TU14_recusaComposicaoComDisciplinaRepetida() {
        var id = UUID.randomUUID(); var simulado = mock(Simulado.class); when(simulado.getId()).thenReturn(id);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        var disciplina = UUID.randomUUID(); var item = new ItemComposicao(disciplina, null, (short) 1);
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(false);

        assertThatThrownBy(() -> service.substituirComposicao(id, List.of(item, item)))
                .isInstanceOf(ComposicaoInvalidaException.class).hasMessage("Verifique a composição do simulado.");
        verify(composicoes, never()).deleteBySimuladoId(id);
    }

    @Test void TU15_resumoDaComposicaoComparaSomaComNumeroDeQuestoes() {
        var id = UUID.randomUUID(); var simulado = mock(Simulado.class); var disciplina = mock(Disciplina.class);
        when(simulado.getId()).thenReturn(id); when(simulado.getNumeroQuestoes()).thenReturn((short) 10);
        when(disciplina.getId()).thenReturn(UUID.randomUUID()); when(disciplina.getNome()).thenReturn("Português");
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(false);
        when(disciplinas.findByIdAndExcluidoEmIsNull(disciplina.getId())).thenReturn(Optional.of(disciplina));
        when(questoes.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(any(), any())).thenReturn(12L);
        when(composicoes.saveAll(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resumo = service.substituirComposicao(id, List.of(new ItemComposicao(disciplina.getId(), null, (short) 10)));

        assertThat(resumo.somaComposicao()).isEqualTo(10); assertThat(resumo.numeroQuestoes()).isEqualTo(10);
        assertThat(resumo.fechada()).isTrue(); assertThat(resumo.acervoSuficiente()).isTrue();
    }

    @Test void TU16_criaSimuladoComStatusRascunho() {
        var gerador = mock(GeradorCodigoSimulado.class); when(gerador.gerar()).thenReturn("SM-001");
        var service = new SimuladoAdminService(simulados, gerador, auditoria, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                consulta, new MontadorSimulado(bancas), composicaoService, localizador);
        var requisicao = new RequisicaoSimulado("Simulado teste", null, null, (short) 10, 600, null, null);
        when(simulados.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0)); when(composicoes.findBySimuladoId(any())).thenReturn(List.of());

        var criado = service.criar(requisicao);

        assertThat(criado.status()).isEqualTo(StatusSimulado.RASCUNHO); assertThat(criado.codigo()).isEqualTo("SM-001");
        assertThat(criado.publicadoEm()).isNull(); verify(auditoria).registrar(eq("SIMULADO_CRIADO"), eq("simulado"), any(), any());
    }

    @Test void TU17_atualizaSimuladoQuandoNaoHaTentativaFinalizada() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-010", "Nome antigo", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(false);
        when(simulados.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(composicoes.findBySimuladoId(any())).thenReturn(List.of());
        var requisicao = new RequisicaoSimulado("Nome novo", null, null, (short) 20, 1200, null, null);

        var atualizado = service.atualizar(id, requisicao);

        assertThat(atualizado.nome()).isEqualTo("Nome novo"); assertThat(atualizado.numeroQuestoes()).isEqualTo((short) 20);
        verify(auditoria).registrar(eq("SIMULADO_ATUALIZADO"), eq("simulado"), eq(id), any());
    }

    @Test void TU18_recusaAtualizarSimuladoComTentativaFinalizada() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-011", "Nome", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(true);
        var requisicao = new RequisicaoSimulado("Outro nome", null, null, (short) 10, 600, null, null);

        assertThatThrownBy(() -> service.atualizar(id, requisicao)).isInstanceOf(ConflitoException.class)
                .hasMessage("O simulado possui tentativa finalizada e não pode ser alterado.");
        verify(simulados, never()).save(any());
    }

    @Test void TU19_excluiSimuladoMarcandoExcluidoEm() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-012", "Nome", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(false);

        service.excluir(id);

        assertThat(simulado.getExcluidoEm()).isNotNull();
        verify(simulados).save(simulado); verify(auditoria).registrar(eq("SIMULADO_EXCLUIDO"), eq("simulado"), eq(id), any());
    }

    @Test void TU20_recusaExcluirSimuladoComTentativaFinalizada() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-013", "Nome", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(ConflitoException.class);
        verify(simulados, never()).save(any());
    }

    @Test void TU21_listaSimuladosSemFiltroUsaBuscaPadrao() {
        when(simulados.findByExcluidoEmIsNull(any())).thenReturn(Page.empty());

        var pagina = service.listar(new FiltroSimulado(null, null, null), new Paginacao(0, 20));

        assertThat(pagina.getContent()).isEmpty();
        verify(simulados).findByExcluidoEmIsNull(any());
        verify(simulados, never()).buscarAdministrativos(any(), any(), any(), any(), any());
    }

    @Test void TU22_listaSimuladosComFiltroUsaConsultaAdministrativa() {
        var simulado = new Simulado("SM-014", "Direito", (short) 10, 600, null);
        when(simulados.buscarAdministrativos(eq("Direito"), eq("DISPONIVEL"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(simulado)));
        when(composicoes.findBySimuladoId(any())).thenReturn(List.of());

        var pagina = service.listar(new FiltroSimulado(" Direito ", "DISPONIVEL", null), new Paginacao(0, 20));

        assertThat(pagina.getContent()).hasSize(1); assertThat(pagina.getContent().get(0).nome()).isEqualTo("Direito");
    }

    @Test void TU23_detalhaSimuladoExistente() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-015", "Nome", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(composicoes.findBySimuladoId(any())).thenReturn(List.of());

        var detalhe = service.detalhar(id);

        assertThat(detalhe.codigo()).isEqualTo("SM-015");
    }

    @Test void TU24_recusaCriarSimuladoComJanelaEncerramentoAntesDaAbertura() {
        var abertura = Instant.parse("2026-01-10T00:00:00Z"); var encerramento = Instant.parse("2026-01-01T00:00:00Z");
        var requisicao = new RequisicaoSimulado("Nome valido", null, null, (short) 10, 600, abertura, encerramento);

        assertThatThrownBy(() -> service.criar(requisicao)).isInstanceOf(DadosInvalidosException.class);
        verify(simulados, never()).save(any());
    }

    @Test void TU25_recusaComposicaoComDisciplinaInexistenteOuArquivada() {
        var id = UUID.randomUUID(); var simulado = new Simulado("SM-016", "Nome", (short) 10, 600, null);
        when(simulados.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(simulado));
        when(tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)).thenReturn(false);
        var disciplinaId = UUID.randomUUID();
        when(disciplinas.findByIdAndExcluidoEmIsNull(disciplinaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.substituirComposicao(id, List.of(new ItemComposicao(disciplinaId, null, (short) 10))))
                .isInstanceOf(ComposicaoInvalidaException.class);
        verify(composicoes, never()).saveAll(any());
    }

    @Test void TU26_recusaCriarSimuladoComBancaInexistente() {
        var bancaId = UUID.randomUUID();
        when(bancas.findById(bancaId)).thenReturn(Optional.empty());
        var requisicao = new RequisicaoSimulado("Nome valido", null, bancaId, (short) 10, 600, null, null);

        assertThatThrownBy(() -> service.criar(requisicao)).isInstanceOf(DadosInvalidosException.class)
                .satisfies(excecao -> assertThat(((DadosInvalidosException) excecao).getFieldErrors()).containsKey("bancaId"));
        verify(simulados, never()).save(any());
    }

    @Test void TU27_listaAdminTrazContagemDeTentativasConcluidas() {
        var simulado = new Simulado("SM-017", "Prova", (short) 10, 600, null);
        when(simulados.findByExcluidoEmIsNull(any())).thenReturn(new PageImpl<>(List.of(simulado)));
        when(composicoes.findBySimuladoId(any())).thenReturn(List.of());
        when(tentativas.countBySimuladoIdAndTipoAndStatus(any(), eq(TipoTentativa.VALIDA), eq(StatusTentativa.FINALIZADA))).thenReturn(7L);

        var pagina = service.listar(new FiltroSimulado(null, null, null), new Paginacao(0, 20));

        assertThat(pagina.getContent().get(0).tentativasConcluidas()).isEqualTo(7L);
    }
}
