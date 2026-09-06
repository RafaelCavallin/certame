package br.com.certame.estudo;

import br.com.certame.comum.excecao.SerieVaziaException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MontadorSerie {
    private final SerieRepository series;
    private final Random random;

    public MontadorSerie(SerieRepository series, Random random) {
        this.series = series;
        this.random = random;
    }

    public List<UUID> montar(FiltroSerie filtro, TamanhoSerie tamanho, UUID usuarioId) {
        var elegiveis = new ArrayList<>(series.buscarElegiveis(filtro, usuarioId));
        if (elegiveis.isEmpty()) throw new SerieVaziaException();
        embaralhar(elegiveis);
        return new ArrayList<>(elegiveis.subList(0, Math.min(tamanho.valor(), elegiveis.size())));
    }

    private void embaralhar(List<UUID> elegiveis) { java.util.Collections.shuffle(elegiveis, random); }
}
