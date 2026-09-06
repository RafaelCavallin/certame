package br.com.certame.simulado;

import br.com.certame.questao.QuestaoRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MontadorProvaSimulado {
    private final QuestaoRepository questoes;
    private final Random random;

    public MontadorProvaSimulado(QuestaoRepository questoes, Random random) {
        this.questoes = questoes;
        this.random = random;
    }

    public List<UUID> montar(UUID simuladoId, List<SimuladoComposicao> composicao) {
        var serie = new ArrayList<UUID>();
        for (var item : composicao) adicionarQuestoes(item, serie);
        Collections.shuffle(serie, random);
        return serie;
    }

    private void adicionarQuestoes(SimuladoComposicao item, List<UUID> serie) {
        var elegiveis = new ArrayList<>(item.getDificuldade() == null
                ? questoes.buscarIdsPublicadasPorDisciplina(item.getDisciplina().getId())
                : questoes.buscarIdsPublicadasPorDisciplinaEDificuldade(item.getDisciplina().getId(), item.getDificuldade()));
        Collections.shuffle(elegiveis, random);
        serie.addAll(elegiveis.subList(0, item.getQuantidade()));
    }
}
