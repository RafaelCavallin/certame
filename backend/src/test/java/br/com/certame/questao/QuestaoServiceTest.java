package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.certame.catalogo.*;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.PublicacaoInvalidaException;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.questao.dto.*;
import br.com.certame.usuario.*;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestaoServiceTest {
    private QuestaoRepository questoes;
    private QuestaoEstatisticaRepository estatisticas;
    private UsuarioRepository usuarios;
    private GeradorCodigoQuestao gerador;
    private DisciplinaRepository disciplinas;
    private AssuntoRepository assuntos;
    private QuestaoService service;
    private final UUID autorId = UUID.randomUUID();
    private final java.time.Instant agora = java.time.Instant.parse("2026-01-01T12:00:00Z");
    private final Clock relogio = Clock.fixed(agora, java.time.ZoneOffset.UTC);

    @BeforeEach
    void preparar() {
        questoes = mock(QuestaoRepository.class);
        estatisticas = mock(QuestaoEstatisticaRepository.class);
        usuarios = mock(UsuarioRepository.class);
        gerador = mock(GeradorCodigoQuestao.class);
        disciplinas = mock(DisciplinaRepository.class);
        assuntos = mock(AssuntoRepository.class);
        var montador = new MontadorQuestao(disciplinas, assuntos, mock(BancaRepository.class), mock(OrgaoRepository.class));
        service = new QuestaoService(questoes, estatisticas, usuarios, gerador, montador,
                mock(RegistradorAuditoria.class), new ValidadorPublicacao(), relogio);
    }

    @Test
    void criaRascunhoMinimoComAutorECodigo() {
        var autor = new Usuario("Professor", "professor@teste", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        when(usuarios.findById(autorId)).thenReturn(Optional.of(autor));
        when(gerador.gerar()).thenReturn("Q-10000");
        when(questoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var detalhe = service.criar(new RequisicaoQuestao(TipoQuestao.ME, "Enunciado de teste", null, null, null, null, null, null, null, null, null), autorId);

        assertThat(detalhe.status()).isEqualTo(StatusQuestao.RASCUNHO);
        assertThat(detalhe.codigo()).isEqualTo("Q-10000");
        assertThat(detalhe.alternativas()).isEmpty();
        verify(estatisticas).save(any(QuestaoEstatistica.class));
    }

    @Test
    void recusaMaisDeCincoAlternativasAntesDePersistir() {
        var alternativas = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(i -> new AlternativaDto(null, "Alternativa " + i, (short) i)).toList();
        var requisicao = new RequisicaoQuestao(TipoQuestao.ME, "Enunciado de teste", null, null, null, null, null, null, alternativas, null, null);

        assertThatThrownBy(() -> service.criar(requisicao, autorId))
                .isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(questoes, usuarios, gerador);
    }

    @Test
    void normalizaLetrasPelaOrdemRecebida() {
        var autor = new Usuario("Professor", "professor@teste", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        when(usuarios.findById(autorId)).thenReturn(Optional.of(autor));
        when(gerador.gerar()).thenReturn("Q-10001");
        when(questoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        var alternativas = List.of(new AlternativaDto("E", "Primeira", (short) 1), new AlternativaDto(null, "Segunda", (short) 2));

        var detalhe = service.criar(new RequisicaoQuestao(TipoQuestao.ME, "Enunciado de teste", null, null, null, null, null, null, alternativas, null, null), autorId);

        assertThat(detalhe.alternativas()).extracting(AlternativaDto::letra).containsExactly("A", "B");
    }

    @Test
    void recusaExclusaoComRespostas() {
        UUID id = UUID.randomUUID();
        Questao questao = new Questao(TipoQuestao.ME, "Enunciado de teste", null);
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));
        when(questoes.countRespostas(id)).thenReturn(1L);

        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(ConflitoException.class)
                .hasMessageContaining("respostas");
        verify(questoes, never()).save(any());
    }

    @Test
    void atualizaRascunhoSubstituindoAsAlternativas() {
        UUID id = UUID.randomUUID();
        var questao = questaoDeTeste();
        questao.adicionarAlternativa(new Alternativa("A", "Antiga", (short) 1));
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));
        when(questoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        var alternativas = List.of(new AlternativaDto(null, "Nova A", (short) 1), new AlternativaDto(null, "Nova B", (short) 2));

        var detalhe = service.atualizar(id, new RequisicaoQuestao(TipoQuestao.ME, "  Enunciado revisado  ", null, null, null, null, null, null, alternativas, null, null));

        assertThat(detalhe.enunciado()).isEqualTo("Enunciado revisado");
        assertThat(detalhe.alternativas()).extracting(AlternativaDto::texto).containsExactly("Nova A", "Nova B");
    }

    @Test
    void atualizarDeMultiplaEscolhaParaCertoErradoLimpaOGabarito() {
        UUID id = UUID.randomUUID();
        var questao = questaoDeTeste();
        questao.setGabarito("A");
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));
        when(questoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var detalhe = service.atualizar(id, new RequisicaoQuestao(TipoQuestao.CE, "Enunciado de teste", null, null, null, null, null, null, List.of(), "A", null));

        assertThat(detalhe.gabarito()).isNull();
        assertThat(detalhe.alternativas()).isEmpty();
    }

    @Test
    void atualizarQuestaoPublicadaIncompletaRecusaComPendencias() {
        UUID id = UUID.randomUUID();
        var questao = questaoDeTeste();
        questao.setStatus(StatusQuestao.PUBLICADA);
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));
        var requisicao = new RequisicaoQuestao(TipoQuestao.ME, "Enunciado de teste", null, null, null, null, null, null, List.of(), null, null);

        assertThatThrownBy(() -> service.atualizar(id, requisicao))
                .isInstanceOf(PublicacaoInvalidaException.class);
        verify(questoes, never()).save(any());
    }

    @Test
    void recusaAssuntoQueNaoPertenceADisciplinaSelecionada() {
        UUID id = UUID.randomUUID();
        UUID disciplinaId = UUID.randomUUID();
        UUID assuntoId = UUID.randomUUID();
        var selecionada = mock(Disciplina.class);
        var outra = mock(Disciplina.class);
        when(selecionada.getId()).thenReturn(disciplinaId);
        when(outra.getId()).thenReturn(UUID.randomUUID());
        var assunto = mock(Assunto.class);
        when(assunto.getDisciplina()).thenReturn(outra);
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questaoDeTeste()));
        when(disciplinas.findByIdAndExcluidoEmIsNull(disciplinaId)).thenReturn(Optional.of(selecionada));
        when(assuntos.findByIdAndExcluidoEmIsNull(assuntoId)).thenReturn(Optional.of(assunto));
        var requisicao = new RequisicaoQuestao(TipoQuestao.ME, "Enunciado de teste", disciplinaId, assuntoId, null, null, null, null, List.of(), null, null);

        assertThatThrownBy(() -> service.atualizar(id, requisicao))
                .isInstanceOf(DadosInvalidosException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(DadosInvalidosException.class))
                .extracting(DadosInvalidosException::getFieldErrors)
                .isEqualTo(java.util.Map.of("assuntoId", "O assunto escolhido não pertence à disciplina selecionada."));
    }

    @Test
    void buscaPorIdDevolveODetalheDaQuestaoAtiva() {
        UUID id = UUID.randomUUID();
        var questao = questaoDeTeste();
        questao.setCodigo("Q-10002");
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));

        assertThat(service.buscarPorId(id).codigo()).isEqualTo("Q-10002");
    }

    @Test
    void buscaPorIdInexistenteRecusa() {
        UUID id = UUID.randomUUID();
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluiQuestaoSemRespostasMarcandoADataDeExclusao() {
        UUID id = UUID.randomUUID();
        var questao = questaoDeTeste();
        when(questoes.findByIdAndExcluidoEmIsNull(id)).thenReturn(Optional.of(questao));
        when(questoes.countRespostas(id)).thenReturn(0L);

        service.excluir(id);

        assertThat(questao.getExcluidoEm()).isEqualTo(agora);
        verify(questoes).save(questao);
    }

    @Test
    void contaDisponiveisPorDisciplinaSemFiltrarDificuldade() {
        UUID disciplinaId = UUID.randomUUID();
        when(questoes.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(disciplinaId, StatusQuestao.PUBLICADA)).thenReturn(7L);

        assertThat(service.contarDisponiveis(disciplinaId)).isEqualTo(7L);
    }

    @Test
    void contaDisponiveisPorDisciplinaEDificuldade() {
        UUID disciplinaId = UUID.randomUUID();
        when(questoes.countByDisciplinaIdAndDificuldadeAndStatusAndExcluidoEmIsNull(disciplinaId, Dificuldade.FACIL, StatusQuestao.PUBLICADA))
                .thenReturn(3L);

        assertThat(service.contarDisponiveis(disciplinaId, Dificuldade.FACIL)).isEqualTo(3L);
    }

    private Questao questaoDeTeste() {
        var autor = new Usuario("Professor", "professor@teste", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        return new Questao(TipoQuestao.ME, "Enunciado de teste", autor);
    }
}
