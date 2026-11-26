package br.com.certame.importacao.triagem;

import br.com.certame.importacao.AlternativaExterna;
import br.com.certame.importacao.QuestaoExterna;
import br.com.certame.questao.TipoQuestao;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** RF-43: espelha MontadorQuestao — alternativas contíguas sem lacuna, nunca compactadas. */
@Component
public class ValidadorEstrutura {
    private static final int MINIMO_ALTERNATIVAS = 2;
    private static final int MAXIMO_ALTERNATIVAS = 5;
    private static final Set<String> GABARITOS_CERTO = Set.of("C", "CERTO", "V", "VERDADEIRO");
    private static final Set<String> GABARITOS_ERRADO = Set.of("E", "ERRADO", "F", "FALSO");

    public record Resultado(boolean valida, String motivo, String gabaritoNormalizado) {}

    public Resultado validar(QuestaoExterna questao, TipoQuestao tipo) {
        return tipo == TipoQuestao.CE ? validarCertoErrado(questao) : validarMultiplaEscolha(questao);
    }

    private Resultado validarCertoErrado(QuestaoExterna questao) {
        var normalizado = normalizarGabaritoCe(questao.gabarito());
        return normalizado.isPresent() ? new Resultado(true, null, normalizado.get())
                : new Resultado(false, "O gabarito não corresponde a certo ou errado.", null);
    }

    private Resultado validarMultiplaEscolha(QuestaoExterna questao) {
        var preenchidas = semLacuna(ordenadas(questao.alternativas()));
        if (preenchidas.isEmpty()) {
            return new Resultado(false, "As alternativas têm uma lacuna no meio da sequência.", null);
        }
        if (preenchidas.size() < MINIMO_ALTERNATIVAS || preenchidas.size() > MAXIMO_ALTERNATIVAS) {
            return new Resultado(false, "A questão deve ter entre duas e cinco alternativas.", null);
        }
        var gabaritoValido = preenchidas.stream().anyMatch(a -> a.letra().equalsIgnoreCase(questao.gabarito()));
        return gabaritoValido ? new Resultado(true, null, questao.gabarito())
                : new Resultado(false, "O gabarito não corresponde a nenhuma alternativa informada.", null);
    }

    private List<AlternativaExterna> ordenadas(List<AlternativaExterna> alternativas) {
        return alternativas == null ? List.of()
                : alternativas.stream().sorted(Comparator.comparing(AlternativaExterna::ordem)).toList();
    }

    private List<AlternativaExterna> semLacuna(List<AlternativaExterna> ordenadas) {
        var preenchidas = new ArrayList<AlternativaExterna>();
        var encontrouLacuna = false;
        for (var alternativa : ordenadas) {
            var temTexto = alternativa.texto() != null && !alternativa.texto().isBlank();
            if (temTexto && encontrouLacuna) {
                return List.of();
            }
            if (temTexto) {
                preenchidas.add(alternativa);
            } else {
                encontrouLacuna = true;
            }
        }
        return preenchidas;
    }

    private Optional<String> normalizarGabaritoCe(String gabarito) {
        if (gabarito == null) {
            return Optional.empty();
        }
        var valor = gabarito.trim().toUpperCase();
        if (GABARITOS_CERTO.contains(valor)) {
            return Optional.of("CERTO");
        }
        if (GABARITOS_ERRADO.contains(valor)) {
            return Optional.of("ERRADO");
        }
        return Optional.empty();
    }
}
