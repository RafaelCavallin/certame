package br.com.certame.flashcard;

import br.com.certame.questao.Questao;
import java.util.Optional;
import java.util.UUID;

public interface QuestaoPublicada {
    Optional<Questao> buscarPublicada(UUID questaoId);
}
