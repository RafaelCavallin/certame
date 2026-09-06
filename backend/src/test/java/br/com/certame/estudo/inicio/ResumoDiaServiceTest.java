package br.com.certame.estudo.inicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.certame.estudo.*;
import br.com.certame.estudo.dto.ResumoInicio;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ResumoDiaServiceTest {
    private static final UUID USUARIO = UUID.randomUUID();
    private static final LocalDate DIA = LocalDate.of(2026, 8, 31);
    private final SessaoEstudoRepository sessoes = mock(SessaoEstudoRepository.class);
    private final RespostaRepository respostas = mock(RespostaRepository.class);
    private final TempoSessao tempo = mock(TempoSessao.class);
    private final TituloSessaoService titulos = mock(TituloSessaoService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-31T15:00:00Z"), ZoneOffset.UTC);

    @Test void TU34_somaResolvidasPercentualETempoDoDia() {
        var inicio = DIA.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant();
        var sessao = sessao(inicio);
        var certa = resposta(true);
        var errada = resposta(false);
        var outraCerta = resposta(true);
        when(respostas.findByUsuarioIdAndRespondidaEmGreaterThanEqualAndRespondidaEmLessThan(eq(USUARIO), any(), any())).thenReturn(List.of(certa, errada, outraCerta));
        when(sessoes.findByUsuarioIdAndIniciadaEmGreaterThanEqualAndIniciadaEmLessThan(eq(USUARIO), any(), any())).thenReturn(List.of(sessao));
        when(tempo.duracaoSegundos(any(), any(), any(), anyInt())).thenReturn(90);

        var resumo = service(null).doDia(USUARIO, DIA);

        assertThat(resumo.questoesResolvidas()).isEqualTo(3);
        assertThat(resumo.pctAcerto()).isEqualTo(67);
        assertThat(resumo.tempoQuestoesSegundos()).isEqualTo(90);
    }
    @Test void TU35_contaSessaoEmAndamentoPelaUltimaAtividade() {
        var sessao = sessao(Instant.parse("2026-08-31T12:00:00Z"));
        when(sessoes.findByUsuarioIdAndIniciadaEmGreaterThanEqualAndIniciadaEmLessThan(eq(USUARIO), any(), any())).thenReturn(List.of(sessao));
        when(sessoes.findByUsuarioIdAndModoNotAndFinalizadaEmIsNullAndExpiradaEmIsNullOrderByUltimaAtividadeEmDesc(USUARIO, ModoSessao.PROVA)).thenReturn(List.of(sessao));
        when(respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId())).thenReturn(List.of());
        when(tempo.duracaoSegundos(any(), isNull(), any(), anyInt())).thenReturn(180);

        var resumo = service(null).doDia(USUARIO, DIA);

        assertThat(resumo.tempoQuestoesSegundos()).isEqualTo(180);
        assertThat(resumo.sessoesRetomaveis()).hasSize(1);
    }
    @Test void TU36_somaTempoDeFlashcardsPelaPorta() {
        var externo = tempoExterno();
        when(externo.segundosNoDia(USUARIO, DIA)).thenReturn(35);

        var resumo = service(externo).doDia(USUARIO, DIA);

        assertThat(resumo.tempoFlashcardsSegundos()).isEqualTo(35);
        assertThat(resumo.tempoEstudoSegundos()).isEqualTo(35);
    }
    @Test void TU37_usaZeroQuandoPortaNaoTemImplementacao() {
        var resumo = service(null).doDia(USUARIO, DIA);

        assertThat(resumo.tempoFlashcardsSegundos()).isZero();
    }
    @Test void TU38_retornaResumoSemAtividade() {
        var resumo = service(null).doDia(USUARIO, DIA);

        assertThat(resumo.questoesResolvidas()).isZero();
        assertThat(resumo.pctAcerto()).isNull();
        assertThat(resumo.sessoesRetomaveis()).isEmpty();
    }
    @Test void TU39_usaSaoPauloNoCorteDoDia() {
        service(null).doDia(USUARIO, DIA);

        verify(respostas).findByUsuarioIdAndRespondidaEmGreaterThanEqualAndRespondidaEmLessThan(USUARIO, Instant.parse("2026-08-31T03:00:00Z"), Instant.parse("2026-09-01T03:00:00Z"));
    }
    private ResumoDiaService service(TempoEstudoExterno externo) { return new ResumoDiaService(sessoes, respostas, tempo, externo, clock, titulos); }
    private SessaoEstudo sessao(Instant inicio) { var sessao = mock(SessaoEstudo.class); when(sessao.getId()).thenReturn(UUID.randomUUID()); when(sessao.getIniciadaEm()).thenReturn(inicio); when(sessao.getPausadoMs()).thenReturn(0); when(sessao.getUltimaAtividadeEm()).thenReturn(inicio); when(sessao.getModo()).thenReturn(ModoSessao.FOCO); when(sessao.getOrigem()).thenReturn(OrigemSessao.LIVRE); when(sessao.getTamanho()).thenReturn((short) 10); return sessao; }
    private Resposta resposta(boolean correta) { var resposta = mock(Resposta.class); when(resposta.isCorreta()).thenReturn(correta); return resposta; }
    private TempoEstudoExterno tempoExterno() { return mock(TempoEstudoExterno.class); }
}
