package br.com.certame.desempenho;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeletorPontosFracosTest {
    private final SeletorPontosFracos seletor = new SeletorPontosFracos();

    @Test void TU09_incluiAssuntoComVinteECincoRespostas() {
        var item = assunto(25, 50);
        assertThat(seletor.selecionar(List.of(item), 70)).containsExactly(new PontoFraco(item.assuntoId(), "Assunto", item.disciplinaId(), "Disciplina", 25, 50));
    }

    @Test void TU10_excluiAssuntoComDezenoveRespostas() {
        assertThat(seletor.selecionar(List.of(assunto(19, 1)), 70)).isEmpty();
    }

    @Test void TU11_incluiAssuntoNoLimiteDeVinteRespostas() {
        assertThat(seletor.selecionar(List.of(assunto(20, 1)), 70)).hasSize(1);
    }

    @Test void TU12_excluiAssuntoAcimaDaMedia() {
        assertThat(seletor.selecionar(List.of(assunto(80, 90)), 70)).isEmpty();
    }

    @Test void TU13_ordenaPorImpacto() {
        var menorPercentual = assunto(20, 38);
        var maiorImpacto = assunto(64, 41);
        assertThat(seletor.selecionar(List.of(menorPercentual, maiorImpacto), 70))
                .extracting(PontoFraco::assuntoId).containsExactly(maiorImpacto.assuntoId(), menorPercentual.assuntoId());
    }

    @Test void TU14_devolveListaVaziaComMediaNula() {
        assertThat(seletor.selecionar(List.of(assunto(50, 10)), null)).isEmpty();
    }

    private TotalPorAssunto assunto(int volume, int pctAcerto) {
        return new TotalPorAssunto(UUID.randomUUID(), "Assunto", UUID.randomUUID(), "Disciplina", volume, pctAcerto);
    }
}
