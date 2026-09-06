package br.com.certame.estudo;

import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.Map;

public record TamanhoSerie(int valor) {
    private static final int TAMANHO_PEQUENO = 10;
    private static final int TAMANHO_MEDIO = 30;
    private static final int TAMANHO_GRANDE = 50;

    public TamanhoSerie {
        if (valor != TAMANHO_PEQUENO && valor != TAMANHO_MEDIO && valor != TAMANHO_GRANDE) {
            throw new DadosInvalidosException(Map.of("tamanho", "Escolha 10, 30 ou 50 questões."));
        }
    }
}
