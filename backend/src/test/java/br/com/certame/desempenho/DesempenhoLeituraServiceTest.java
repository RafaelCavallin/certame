package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.certame.catalogo.Assunto;
import br.com.certame.catalogo.AssuntoRepository;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.DisciplinaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DesempenhoLeituraServiceTest {
    private static final UUID USUARIO = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void TU36_resumoSemHistorico() {
        var repositorio = mock(DesempenhoDisciplinaRepository.class);
        when(repositorio.findByIdUsuarioIdOrderByVolumeDesc(USUARIO)).thenReturn(List.of());

        var resultado = new ResumoDesempenhoService(repositorio, new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.totalRespostas()).isZero();
        assertThat(resultado.acertos()).isZero();
        assertThat(resultado.erros()).isZero();
        assertThat(resultado.pctAproveitamento()).isNull();
        assertThat(resultado.possuiHistorico()).isFalse();
    }

    @Test
    void TU37_resumoDerivaErrosEAproveitamento() {
        var repositorio = mock(DesempenhoDisciplinaRepository.class);
        var direito = mock(DesempenhoDisciplina.class);
        var portugues = mock(DesempenhoDisciplina.class);
        when(direito.getVolume()).thenReturn(3);
        when(direito.getAcertos()).thenReturn(2);
        when(portugues.getVolume()).thenReturn(2);
        when(portugues.getAcertos()).thenReturn(1);
        when(repositorio.findByIdUsuarioIdOrderByVolumeDesc(USUARIO)).thenReturn(List.of(direito, portugues));

        var resultado = new ResumoDesempenhoService(repositorio, new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.totalRespostas()).isEqualTo(5);
        assertThat(resultado.acertos()).isEqualTo(3);
        assertThat(resultado.erros()).isEqualTo(2);
        assertThat(resultado.pctAproveitamento()).isEqualTo(60);
        assertThat(resultado.possuiHistorico()).isTrue();
    }

    @Test
    void TU06_evolucaoCalculaMediaPonderadaIgnorandoLacunas() {
        var repositorio = mock(DesempenhoSemanalRepository.class);
        var semana = semana(LocalDate.of(2026, 8, 24), 10, 8);
        when(repositorio.findByIdUsuarioIdAndIdSemanaBetweenOrderByIdSemanaAsc(eq(USUARIO), any(), any()))
                .thenReturn(List.of(semana));
        var calendario = Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"), ZoneOffset.UTC);

        var resultado = new EvolucaoSemanalService(repositorio, calendario, new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.semanas()).hasSize(8);
        assertThat(resultado.pctMediaPeriodo()).isEqualTo(80);
        assertThat(resultado.semanas().get(0).pctAcerto()).isNull();
    }

    @Test
    void TU07_evolucaoDevolveMelhorSemanaEVolumeTotal() {
        var repositorio = mock(DesempenhoSemanalRepository.class);
        var semanaAnterior = semana(LocalDate.of(2026, 8, 17), 5, 3);
        var semanaAtual = semana(LocalDate.of(2026, 8, 24), 10, 10);
        when(repositorio.findByIdUsuarioIdAndIdSemanaBetweenOrderByIdSemanaAsc(eq(USUARIO), any(), any()))
                .thenReturn(List.of(semanaAnterior, semanaAtual));

        var resultado = new EvolucaoSemanalService(repositorio,
                Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"), ZoneOffset.UTC), new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.volumeTotal()).isEqualTo(15);
        assertThat(resultado.melhorSemana().semana()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(resultado.melhorSemana().pctAcerto()).isEqualTo(100);
    }

    @Test
    void TU08_evolucaoSemAtividade() {
        var repositorio = mock(DesempenhoSemanalRepository.class);
        when(repositorio.findByIdUsuarioIdAndIdSemanaBetweenOrderByIdSemanaAsc(eq(USUARIO), any(), any()))
                .thenReturn(List.of());

        var resultado = new EvolucaoSemanalService(repositorio,
                Clock.fixed(Instant.parse("2026-08-30T15:00:00Z"), ZoneOffset.UTC), new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.semanas()).hasSize(8).allSatisfy(semana -> {
            assertThat(semana.volume()).isZero();
            assertThat(semana.pctAcerto()).isNull();
        });
        assertThat(resultado.pctMediaPeriodo()).isNull();
        assertThat(resultado.melhorSemana()).isNull();
        assertThat(resultado.volumeTotal()).isZero();
    }

    @Test
    void TU39_pontosFracosAplicaMediaEMinimo() {
        var assuntoRepositorio = mock(AssuntoRepository.class);
        var disciplinaRepositorio = mock(DesempenhoDisciplinaRepository.class);
        var desempenhoRepositorio = mock(DesempenhoAssuntoRepository.class);
        var disciplina = mock(Disciplina.class);
        var assunto = mock(Assunto.class);
        var assuntoId = UUID.randomUUID();
        var disciplinaId = UUID.randomUUID();
        when(assuntoRepositorio.findById(assuntoId)).thenReturn(Optional.of(assunto));
        when(assunto.getNome()).thenReturn("Licitações");
        when(assunto.getDisciplina()).thenReturn(disciplina);
        when(disciplina.getNome()).thenReturn("Direito Administrativo");
        var desempenhoDisciplina = desempenho(100, 70);
        when(disciplinaRepositorio.findByIdUsuarioIdOrderByVolumeDesc(USUARIO)).thenReturn(List.of(desempenhoDisciplina));
        var fraco = desempenhoAssunto(assuntoId, disciplinaId, 25, 10);
        when(desempenhoRepositorio.findByIdUsuarioIdAndVolumeGreaterThanEqualOrderByVolumeDesc(USUARIO, 20))
                .thenReturn(List.of(fraco));

        var resultado = new PontosFracosService(desempenhoRepositorio, disciplinaRepositorio, assuntoRepositorio,
                new SimpleMeterRegistry()).consultar(USUARIO);

        assertThat(resultado.pctMediaUsuario()).isEqualTo(70);
        assertThat(resultado.minimoRespostas()).isEqualTo(20);
        assertThat(resultado.itens()).extracting("volume").containsExactly(25);
    }

    private DesempenhoSemanal semana(LocalDate data, int volume, int acertos) {
        var semana = mock(DesempenhoSemanal.class);
        when(semana.getSemana()).thenReturn(data);
        when(semana.getVolume()).thenReturn(volume);
        when(semana.getAcertos()).thenReturn(acertos);
        return semana;
    }

    private DesempenhoDisciplina desempenho(int volume, int acertos) {
        var desempenho = mock(DesempenhoDisciplina.class);
        when(desempenho.getVolume()).thenReturn(volume);
        when(desempenho.getAcertos()).thenReturn(acertos);
        return desempenho;
    }

    private DesempenhoAssunto desempenhoAssunto(UUID assuntoId, UUID disciplinaId, int volume, int acertos) {
        var desempenho = mock(DesempenhoAssunto.class);
        when(desempenho.getAssuntoId()).thenReturn(assuntoId);
        when(desempenho.getDisciplinaId()).thenReturn(disciplinaId);
        when(desempenho.getVolume()).thenReturn(volume);
        when(desempenho.getPctAcerto()).thenReturn((short) Math.round(acertos * 100.0 / volume));
        return desempenho;
    }
}
