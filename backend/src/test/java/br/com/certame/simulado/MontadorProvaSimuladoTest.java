package br.com.certame.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.questao.QuestaoRepository;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MontadorProvaSimuladoTest {
    @Test
    void TU03_montaSerieApenasComQuestoesPublicadas() {
        var disciplina = disciplina("Direito");
        var publicadas = ids("direito", 3);
        var repositorio = mock(QuestaoRepository.class);
        when(repositorio.buscarIdsPublicadasPorDisciplina(disciplina.getId())).thenReturn(publicadas);
        var composicao = composicao(disciplina, 3);

        var serie = new MontadorProvaSimulado(repositorio, new Random(7)).montar(UUID.randomUUID(), List.of(composicao));

        assertThat(serie).containsExactlyInAnyOrderElementsOf(publicadas);
    }

    @Test
    void TU04_respeitaQuantidadePorDisciplinaNaComposicao() {
        var direito = disciplina("Direito");
        var portugues = disciplina("Português");
        var repositorio = mock(QuestaoRepository.class);
        var questoesDireito = ids("direito", 4);
        var questoesPortugues = ids("portugues", 6);
        when(repositorio.buscarIdsPublicadasPorDisciplina(direito.getId())).thenReturn(questoesDireito);
        when(repositorio.buscarIdsPublicadasPorDisciplina(portugues.getId())).thenReturn(questoesPortugues);

        var serie = new MontadorProvaSimulado(repositorio, new Random(7)).montar(UUID.randomUUID(),
                List.of(composicao(direito, 3), composicao(portugues, 2)));

        assertThat(serie).hasSize(5).doesNotHaveDuplicates();
        assertThat(serie).filteredOn(questoesDireito::contains).hasSize(3);
        assertThat(serie).filteredOn(questoesPortugues::contains).hasSize(2);
    }

    private Disciplina disciplina(String nome) {
        var disciplina = mock(Disciplina.class);
        when(disciplina.getId()).thenReturn(UUID.nameUUIDFromBytes(nome.getBytes()));
        when(disciplina.getNome()).thenReturn(nome);
        return disciplina;
    }

    private SimuladoComposicao composicao(Disciplina disciplina, int quantidade) {
        var item = mock(SimuladoComposicao.class);
        when(item.getDisciplina()).thenReturn(disciplina);
        when(item.getQuantidade()).thenReturn((short) quantidade);
        return item;
    }

    private List<UUID> ids(String prefixo, int quantidade) {
        return java.util.stream.IntStream.range(0, quantidade)
                .mapToObj(indice -> UUID.nameUUIDFromBytes((prefixo + "-" + indice).getBytes()))
                .toList();
    }
}
