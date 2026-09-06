package br.com.certame.simulado.dto;

import br.com.certame.questao.Dificuldade;
import java.util.List;
import java.util.UUID;

public record ResumoComposicao(List<ItemComposicaoResumo> itens, int somaComposicao, int numeroQuestoes,
                               boolean fechada, boolean acervoSuficiente) {
    public record ItemComposicaoResumo(UUID disciplinaId, String disciplina, Dificuldade dificuldade,
                                       int quantidade, long disponiveis) {}
}
