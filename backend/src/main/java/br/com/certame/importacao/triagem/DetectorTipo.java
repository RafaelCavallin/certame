package br.com.certame.importacao.triagem;

import br.com.certame.importacao.QuestaoExterna;
import br.com.certame.questao.TipoQuestao;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** RF-42: recusa o ambíguo em vez de adivinhar entre múltipla escolha e certo/errado. */
@Component
public class DetectorTipo {
    private static final int MINIMO_ALTERNATIVAS_ME = 2;
    private static final Set<String> GABARITOS_CERTO_ERRADO =
            Set.of("C", "E", "CERTO", "ERRADO", "V", "F", "VERDADEIRO", "FALSO");

    public Optional<TipoQuestao> detectar(QuestaoExterna questao) {
        var preenchidas = contarAlternativasPreenchidas(questao);
        if (preenchidas >= MINIMO_ALTERNATIVAS_ME) {
            return Optional.of(TipoQuestao.ME);
        }
        if (preenchidas == 0 && ehGabaritoCertoErrado(questao.gabarito())) {
            return Optional.of(TipoQuestao.CE);
        }
        return Optional.empty();
    }

    private long contarAlternativasPreenchidas(QuestaoExterna questao) {
        return questao.alternativas() == null ? 0
                : questao.alternativas().stream().filter(a -> a.texto() != null && !a.texto().isBlank()).count();
    }

    private boolean ehGabaritoCertoErrado(String gabarito) {
        return gabarito != null && GABARITOS_CERTO_ERRADO.contains(gabarito.trim().toUpperCase());
    }
}
