package br.com.certame.estudo;

import br.com.certame.questao.Dificuldade;
import java.util.UUID;

public record FiltroSerie(UUID disciplinaId, UUID assuntoId, UUID bancaId, Short ano,
                          Dificuldade dificuldade, Boolean somenteIneditas, Boolean excluirAcertadas) {
    /** Os dois campos são opcionais no JSON de entrada (ex.: `filtro: { assuntoId }` do "Treinar"); sem
     * isso o Jackson recusa o corpo com 400 ao tentar mapear a chave ausente num `boolean` primitivo. */
    public FiltroSerie {
        somenteIneditas = somenteIneditas != null && somenteIneditas;
        excluirAcertadas = excluirAcertadas != null && excluirAcertadas;
    }
}
