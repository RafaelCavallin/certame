package br.com.certame.questao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.certame.estudo.RespostaEstatisticaRepository;
import br.com.certame.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstatisticaQuestaoServiceTest {
    private final UUID questaoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();
    private QuestaoRepository questoes;
    private RespostaEstatisticaRepository respostas;
    private QuestaoEstatisticaRepository estatisticas;
    private EstatisticaQuestaoService service;

    @BeforeEach
    void preparar() {
        questoes = mock(QuestaoRepository.class);
        respostas = mock(RespostaEstatisticaRepository.class);
        estatisticas = mock(QuestaoEstatisticaRepository.class);
        service = new EstatisticaQuestaoService(questoes, respostas, estatisticas,
                Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void recalculaExcluindoRespostasDoAutor() {
        var questao = mock(Questao.class);
        var autor = mock(Usuario.class);
        when(questao.getAutor()).thenReturn(autor);
        when(questao.getId()).thenReturn(questaoId);
        when(autor.getId()).thenReturn(autorId);
        when(questoes.findByIdAndExcluidoEmIsNull(questaoId)).thenReturn(Optional.of(questao));
        when(respostas.contarValidas(questaoId, autorId)).thenReturn(new RespostaEstatisticaRepository.Agregado(3, 2, 1200));
        when(estatisticas.findById(questaoId)).thenReturn(Optional.empty());
        when(estatisticas.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = service.recalcular(questaoId);

        assertThat(resultado.totalRespostas()).isEqualTo(3);
        assertThat(resultado.totalAcertos()).isEqualTo(2);
        assertThat(resultado.pctAcerto()).isEqualTo((short) 67);
        assertThat(resultado.atualizadoEm()).isEqualTo(Instant.parse("2026-08-30T15:00:00Z"));
        verify(respostas).contarValidas(questaoId, autorId);
    }

    @Test
    void recalculaSemRespostasDeTerceirosSemDivisaoPorZero() {
        var questao = mock(Questao.class);
        var autor = mock(Usuario.class);
        when(questao.getAutor()).thenReturn(autor);
        when(questao.getId()).thenReturn(questaoId);
        when(autor.getId()).thenReturn(autorId);
        when(questoes.findByIdAndExcluidoEmIsNull(questaoId)).thenReturn(Optional.of(questao));
        when(respostas.contarValidas(questaoId, autorId)).thenReturn(new RespostaEstatisticaRepository.Agregado(0, 0, null));
        when(estatisticas.findById(questaoId)).thenReturn(Optional.empty());
        when(estatisticas.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = service.recalcular(questaoId);

        assertThat(resultado.totalRespostas()).isZero();
        assertThat(resultado.totalAcertos()).isZero();
        assertThat(resultado.pctAcerto()).isNull();
    }
}
