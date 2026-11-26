package br.com.certame.importacao.triagem;

import br.com.certame.importacao.ResultadoCandidata;
import br.com.certame.questao.TipoQuestao;
import java.util.List;

public record ResultadoTriagem(boolean aprovada, ResultadoCandidata resultado, String motivo,
        TipoQuestao tipo, String gabaritoNormalizado, List<String> pendencias) {

    public static ResultadoTriagem aprovada(TipoQuestao tipo, String gabaritoNormalizado, List<String> pendencias) {
        return new ResultadoTriagem(true, null, "Questão compatível com o modelo do Certame.", tipo, gabaritoNormalizado, pendencias);
    }

    public static ResultadoTriagem reprovada(ResultadoCandidata resultado, String motivo) {
        return new ResultadoTriagem(false, resultado, motivo, null, null, List.of());
    }
}
