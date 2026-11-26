package br.com.certame.importacao;

import br.com.certame.questao.Fonte;
import java.util.List;

public record QuestaoExterna(
        Fonte fonte, String identificadorExterno, String enunciado, List<AlternativaExterna> alternativas,
        String gabarito, String comentario, String disciplinaExterna, String assuntoExterno,
        String bancaExterna, String orgaoExterno, Short ano, String cargoExterno,
        String referenciaTextoBase, String urlExterna) {
}
