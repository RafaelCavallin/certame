package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.PublicacaoInvalidaException;
import br.com.certame.questao.dto.QuestaoDetalhe;
import br.com.certame.usuario.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicacaoServiceTest {
    private QuestaoRepository questoes;
    private RegistradorAuditoria auditoria;
    private PublicacaoService service;
    private Questao questao;

    @BeforeEach
    void preparar() {
        questoes = mock(QuestaoRepository.class);
        auditoria = mock(RegistradorAuditoria.class);
        service = new PublicacaoService(questoes, new ValidadorPublicacao(), auditoria,
                Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC),
                new MetricasPublicacao(questoes, new SimpleMeterRegistry()));
        var autor = new Usuario("Professor", "professor@teste", "hash", PerfilUsuario.PROFESSOR, StatusUsuario.ATIVO);
        questao = new Questao(TipoQuestao.ME, "Enunciado de teste", autor);
        questao.setDisciplina(new Disciplina("Direito Constitucional"));
        questao.setCodigo("Q-10000");
        when(questoes.findByIdAndExcluidoEmIsNull(any())).thenReturn(Optional.of(questao));
        when(questoes.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void publicaQuestaoValidaComDataEAuditoria() {
        questao.setGabarito("A");
        questao.setComentario("Comentário revisado.");
        questao.adicionarAlternativa(new Alternativa("A", "Uma", (short) 1));
        questao.adicionarAlternativa(new Alternativa("B", "Duas", (short) 2));

        QuestaoDetalhe detalhe = service.publicar(UUID.randomUUID());

        assertThat(detalhe.status()).isEqualTo(StatusQuestao.PUBLICADA);
        assertThat(detalhe.publicadoEm()).isEqualTo(Instant.parse("2026-08-30T12:00:00Z"));
        verify(auditoria).registrar(eq("PUBLICAR"), eq("questao"), any(), any());
    }

    @Test
    void recusaPublicacaoComTodasAsPendencias() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.publicar(id)).isInstanceOf(PublicacaoInvalidaException.class)
                .extracting(Throwable::getMessage).asString().contains("campos pendentes");
        assertThat(questao.getStatus()).isEqualTo(StatusQuestao.RASCUNHO);
        verify(questoes, never()).save(any());
    }

    @Test
    void enviaRascunhoParaRevisao() {
        var detalhe = service.enviarParaRevisao(UUID.randomUUID());

        assertThat(detalhe.status()).isEqualTo(StatusQuestao.EM_REVISAO);
        var payload = ArgumentCaptor.forClass(Object.class);
        verify(auditoria).registrar(eq("ENVIAR_REVISAO"), eq("questao"), any(), payload.capture());
        assertThat(payload.getValue().toString()).contains("statusAnterior", "statusNovo");
    }

    @Test
    void despublicaParaRevisaoSemAlterarData() {
        var publicadoEm = Instant.parse("2026-08-01T10:00:00Z");
        questao.setStatus(StatusQuestao.PUBLICADA);
        questao.setPublicadoEm(publicadoEm);

        var detalhe = service.despublicar(UUID.randomUUID());

        assertThat(detalhe.status()).isEqualTo(StatusQuestao.EM_REVISAO);
        assertThat(detalhe.publicadoEm()).isEqualTo(publicadoEm);
        verify(auditoria).registrar(eq("DESPUBLICAR"), eq("questao"), any(), any());
    }

    @Test
    void recusaDespublicacaoDeRascunho() {
        assertThatThrownBy(() -> service.despublicar(UUID.randomUUID()))
                .isInstanceOf(ConflitoException.class).hasMessageContaining("transição");
        verify(questoes, never()).save(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void recusaEnvioParaRevisaoDeQuestaoPublicada() {
        questao.setStatus(StatusQuestao.PUBLICADA);

        assertThatThrownBy(() -> service.enviarParaRevisao(UUID.randomUUID()))
                .isInstanceOf(ConflitoException.class);
        verify(questoes, never()).save(any());
    }
}
