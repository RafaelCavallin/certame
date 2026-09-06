package br.com.certame.estudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.estudo.caderno.Caderno;
import br.com.certame.estudo.caderno.CadernoRepository;
import br.com.certame.simulado.Simulado;
import br.com.certame.simulado.SimuladoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TituloSessaoServiceTest {
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID SESSAO = UUID.randomUUID();
    private final SessaoQuestaoRepository questoes = mock(SessaoQuestaoRepository.class);
    private final CadernoRepository cadernos = mock(CadernoRepository.class);
    private final SimuladoRepository simulados = mock(SimuladoRepository.class);
    private final TituloSessaoService service = new TituloSessaoService(questoes, cadernos, simulados);

    @Test void livreComUmaDisciplinaUsaSeuNome() {
        when(questoes.disciplinasDaSessao(SESSAO)).thenReturn(List.of("Direito Constitucional"));

        var titulo = service.titulo(SESSAO, OrigemSessao.LIVRE, null, USUARIO);

        assertThat(titulo).isEqualTo("Direito Constitucional");
    }

    @Test void livreComVariasDisciplinasUsaRotuloGenerico() {
        when(questoes.disciplinasDaSessao(SESSAO)).thenReturn(List.of("Direito Constitucional", "Português"));

        var titulo = service.titulo(SESSAO, OrigemSessao.LIVRE, null, USUARIO);

        assertThat(titulo).isEqualTo("Questões variadas");
    }

    @Test void livreSemDisciplinaConhecidaUsaRotuloGenerico() {
        when(questoes.disciplinasDaSessao(SESSAO)).thenReturn(List.of());

        var titulo = service.titulo(SESSAO, OrigemSessao.LIVRE, null, USUARIO);

        assertThat(titulo).isEqualTo("Questões variadas");
    }

    @Test void cadernoManualUsaNomeCadastrado() {
        var cadernoId = UUID.randomUUID();
        var caderno = mock(Caderno.class);
        when(caderno.getNome()).thenReturn("Direito Administrativo");
        when(cadernos.findByIdAndUsuarioIdAndExcluidoEmIsNull(cadernoId, USUARIO)).thenReturn(Optional.of(caderno));

        var titulo = service.titulo(SESSAO, OrigemSessao.CADERNO, cadernoId.toString(), USUARIO);

        assertThat(titulo).isEqualTo("Caderno: Direito Administrativo");
    }

    @Test void cadernoManualExcluidoUsaRotuloGenerico() {
        var cadernoId = UUID.randomUUID();
        when(cadernos.findByIdAndUsuarioIdAndExcluidoEmIsNull(cadernoId, USUARIO)).thenReturn(Optional.empty());

        var titulo = service.titulo(SESSAO, OrigemSessao.CADERNO, cadernoId.toString(), USUARIO);

        assertThat(titulo).isEqualTo("Caderno");
    }

    @Test void cadernoAutomaticoDeErrosUsaNomeAmigavel() {
        var titulo = service.titulo(SESSAO, OrigemSessao.CADERNO, "auto:ERRO_RECORRENTE", USUARIO);

        assertThat(titulo).isEqualTo("Caderno: Erros recorrentes");
    }

    @Test void cadernoAutomaticoDeFavoritasUsaNomeAmigavel() {
        var titulo = service.titulo(SESSAO, OrigemSessao.CADERNO, "auto:FAVORITA", USUARIO);

        assertThat(titulo).isEqualTo("Caderno: Favoritas");
    }

    @Test void simuladoUsaNomeCadastrado() {
        var simuladoId = UUID.randomUUID();
        var simulado = mock(Simulado.class);
        when(simulado.getNome()).thenReturn("TJ-SP 2026");
        when(simulados.findByIdAndExcluidoEmIsNull(simuladoId)).thenReturn(Optional.of(simulado));

        var titulo = service.titulo(SESSAO, OrigemSessao.SIMULADO, simuladoId.toString(), USUARIO);

        assertThat(titulo).isEqualTo("Simulado: TJ-SP 2026");
    }

    @Test void simuladoExcluidoUsaRotuloGenerico() {
        var simuladoId = UUID.randomUUID();
        when(simulados.findByIdAndExcluidoEmIsNull(simuladoId)).thenReturn(Optional.empty());

        var titulo = service.titulo(SESSAO, OrigemSessao.SIMULADO, simuladoId.toString(), USUARIO);

        assertThat(titulo).isEqualTo("Simulado");
    }
}
