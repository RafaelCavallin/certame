package br.com.certame.questao;

import br.com.certame.comum.excecao.PublicacaoInvalidaException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Regra pura de completude exigida antes da publicação. */
@Component
public class ValidadorPublicacao {
    public void garantirPublicavel(Questao questao) {
        var pendencias = validar(questao);
        if (!pendencias.isEmpty()) {
            throw new PublicacaoInvalidaException(pendencias);
        }
    }

    public Map<String, String> validar(Questao questao) {
        var pendencias = new LinkedHashMap<String, String>();
        if (questao.getGabarito() == null || questao.getGabarito().isBlank()
                || (questao.getTipo() == TipoQuestao.CE
                    && !questao.getGabarito().equals("CERTO") && !questao.getGabarito().equals("ERRADO"))) {
            pendencias.put("gabarito", "Marque uma opção válida antes de publicar.");
        }
        if (questao.getComentario() == null || questao.getComentario().isBlank()) {
            pendencias.put("comentario", "O comentário do professor é obrigatório para publicar.");
        }
        if (questao.getTipo() == TipoQuestao.ME && questao.getAlternativas().size() < 2) {
            pendencias.put("alternativas", "Informe ao menos duas alternativas.");
        }
        if (questao.getDisciplina() == null) {
            pendencias.put("disciplina", "Selecione uma disciplina antes de publicar.");
        }
        return pendencias;
    }
}
