package br.com.certame.estudo.caderno;

import br.com.certame.comum.excecao.QuestaoNaoPublicadaException;
import br.com.certame.questao.QuestaoRepository;
import br.com.certame.questao.StatusQuestao;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adiciona e remove questões de um caderno manual, validando que estejam publicadas. */
@Component
public class MutadorItensCaderno {
    private final CadernoQuestaoRepository questoesCaderno;
    private final QuestaoRepository questoes;
    private final Clock clock;

    public MutadorItensCaderno(CadernoQuestaoRepository questoesCaderno, QuestaoRepository questoes, Clock clock) {
        this.questoesCaderno = questoesCaderno;
        this.questoes = questoes;
        this.clock = clock;
    }

    public void adicionar(Caderno caderno, List<UUID> ids) {
        validarPublicadas(ids);
        var unicos = new LinkedHashSet<>(ids);
        unicos.forEach(questaoId -> questoesCaderno.adicionar(caderno.getId(), questaoId));
        caderno.tocar(Instant.now(clock));
    }

    public void remover(UUID cadernoId, UUID questaoId, Caderno caderno) {
        questoesCaderno.remover(cadernoId, questaoId);
        caderno.tocar(Instant.now(clock));
    }

    private void validarPublicadas(List<UUID> ids) {
        var naoPublicadas = ids.stream().distinct()
                .filter(id -> questoes.findByIdAndExcluidoEmIsNull(id).map(q -> q.getStatus() != StatusQuestao.PUBLICADA).orElse(true))
                .count();
        if (naoPublicadas > 0) {
            throw new QuestaoNaoPublicadaException((int) naoPublicadas);
        }
    }
}
