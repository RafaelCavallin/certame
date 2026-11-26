package br.com.certame.importacao.triagem;

import br.com.certame.importacao.QuestaoExterna;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** RF-44: enunciado, comentário e ano dentro dos limites do modelo do Certame. */
@Component
public class ValidadorLimites {
    private static final int ENUNCIADO_MINIMO = 10;
    private static final int ENUNCIADO_MAXIMO = 20_000;
    private static final int COMENTARIO_MAXIMO = 20_000;
    private static final int ANO_MINIMO = 1990;
    private static final int ANO_MAXIMO = 2100;

    public Optional<String> validar(QuestaoExterna questao) {
        var enunciado = questao.enunciado() == null ? "" : questao.enunciado().trim();
        if (enunciado.length() < ENUNCIADO_MINIMO || enunciado.length() > ENUNCIADO_MAXIMO) {
            return Optional.of("O enunciado deve ter entre 10 e 20000 caracteres.");
        }
        if (questao.comentario() != null && questao.comentario().length() > COMENTARIO_MAXIMO) {
            return Optional.of("O comentário não pode ultrapassar 20000 caracteres.");
        }
        if (questao.ano() != null && (questao.ano() < ANO_MINIMO || questao.ano() > ANO_MAXIMO)) {
            return Optional.of("O ano deve estar entre 1990 e 2100.");
        }
        return Optional.empty();
    }
}
