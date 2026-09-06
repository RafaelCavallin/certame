package br.com.certame.catalogo.edital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolvedorDisciplinasEditalTest {
    private final DisciplinaRepository disciplinas = mock(DisciplinaRepository.class);
    private final ResolvedorDisciplinasEdital resolvedor = new ResolvedorDisciplinasEdital(disciplinas);

    @Test void TU14_recusaIdQueNaoExiste() {
        // Arrange
        var id = UUID.randomUUID();
        when(disciplinas.findByIdInAndExcluidoEmIsNull(Set.of(id))).thenReturn(java.util.List.of());

        // Act & Assert
        assertThatThrownBy(() -> resolvedor.resolver(Set.of(id)))
                .isInstanceOfSatisfying(DadosInvalidosException.class, excecao ->
                        assertThat(excecao.getFieldErrors())
                                .containsEntry("disciplinaIds",
                                        "Uma das disciplinas selecionadas não existe mais na árvore de matérias."));
    }

    @Test void TU15_recusaDisciplinaExcluidaLogicamente() {
        // Arrange
        var id = UUID.randomUUID();
        when(disciplinas.findByIdInAndExcluidoEmIsNull(Set.of(id))).thenReturn(java.util.List.of());

        // Act & Assert
        assertThatThrownBy(() -> resolvedor.resolver(Set.of(id)))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test void TU16_reduzIdsRepetidosAUmaUnicaDisciplina() {
        // Arrange
        var id = UUID.randomUUID();
        var disciplina = new Disciplina("Direito Constitucional");
        when(disciplinas.findByIdInAndExcluidoEmIsNull(Set.of(id))).thenReturn(java.util.List.of(disciplina));

        // Act
        var resolvidas = resolvedor.resolver(Set.of(id));

        // Assert
        assertThat(resolvidas).containsExactly(disciplina);
        verify(disciplinas).findByIdInAndExcluidoEmIsNull(Set.of(id));
    }

    @Test void TU17_devolveConjuntoVazioSemConsultarRepositorio() {
        // Act
        var resolvidas = resolvedor.resolver(Set.of());

        // Assert
        assertThat(resolvidas).isEmpty();
        verify(disciplinas, never()).findByIdInAndExcluidoEmIsNull(org.mockito.ArgumentMatchers.any());
    }

    @Test void TU17b_resolveEmLoteComUnicaConsultaParaMultiplosIds() {
        // Arrange
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var d1 = new Disciplina("Direito Constitucional");
        var d2 = new Disciplina("Direito Administrativo");
        when(disciplinas.findByIdInAndExcluidoEmIsNull(Set.of(id1, id2))).thenReturn(java.util.List.of(d1, d2));

        // Act
        var resolvidas = resolvedor.resolver(Set.of(id1, id2));

        // Assert
        assertThat(resolvidas).hasSize(2);
        verify(disciplinas).findByIdInAndExcluidoEmIsNull(Set.of(id1, id2));
    }

    @Test void TU17c_resolveComIdsNulosRetornaVazioSemConsulta() {
        // Act
        var resolvidas = resolvedor.resolver(null);

        // Assert
        assertThat(resolvidas).isEmpty();
        verify(disciplinas, never()).findByIdInAndExcluidoEmIsNull(org.mockito.ArgumentMatchers.any());
    }
}
