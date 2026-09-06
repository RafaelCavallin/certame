package br.com.certame.flashcard;

import br.com.certame.estudo.SessaoEstudo;
import br.com.certame.questao.Questao;

public record ContextoOrigem(Questao questao, SessaoEstudo sessao) {
    public static ContextoOrigem vazio() {
        return new ContextoOrigem(null, null);
    }
}
