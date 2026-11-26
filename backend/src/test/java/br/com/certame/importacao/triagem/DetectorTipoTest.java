package br.com.certame.importacao.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certame.importacao.AlternativaExterna;
import br.com.certame.importacao.QuestaoExterna;
import br.com.certame.questao.TipoQuestao;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectorTipoTest {
    private final DetectorTipo detector = new DetectorTipo();

    @Test
    void TU01_detectaMultiplaEscolhaComDuasOuMaisOpcoes() {
        var questao = questaoExterna(List.of(alternativa("A", "Texto A"), alternativa("B", "Texto B")), null);

        assertThat(detector.detectar(questao)).contains(TipoQuestao.ME);
    }

    @Test
    void TU02_detectaCertoErradoSemOpcoesComGabaritoEquivalente() {
        var questao = questaoExterna(List.of(), "C");

        assertThat(detector.detectar(questao)).contains(TipoQuestao.CE);
    }

    @Test
    void naoDetectaTipoQuandoUmaUnicaOpcaoEGabaritoNaoEhCertoErrado() {
        var questao = questaoExterna(List.of(alternativa("A", "Texto A")), "X");

        assertThat(detector.detectar(questao)).isEmpty();
    }

    private AlternativaExterna alternativa(String letra, String texto) {
        return new AlternativaExterna(letra, texto, (short) 1);
    }

    private QuestaoExterna questaoExterna(List<AlternativaExterna> alternativas, String gabarito) {
        return new QuestaoExterna(null, "id-1", "Enunciado de teste suficientemente longo", alternativas,
                gabarito, "Comentário válido com conteúdo suficiente.", null, null, null, null, null, null, null, null);
    }
}
