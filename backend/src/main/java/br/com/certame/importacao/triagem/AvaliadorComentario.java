package br.com.certame.importacao.triagem;

import org.springframework.stereotype.Component;

/** RF-46: comentário sem conteúdo útil vira pendência, nunca descarte. Retorna true quando reprovado. */
@Component
public class AvaliadorComentario {
    private static final int TAMANHO_MINIMO_CONTEUDO = 5;

    public boolean avaliar(String comentario, String enunciado, String gabarito) {
        if (comentario == null || comentario.isBlank()) {
            return true;
        }
        var normalizado = normalizar(comentario);
        if (normalizado.length() < TAMANHO_MINIMO_CONTEUDO) {
            return true;
        }
        if (gabarito != null && normalizado.equals(normalizar(gabarito))) {
            return true;
        }
        return enunciado != null && normalizado.equals(normalizar(enunciado));
    }

    private String normalizar(String texto) {
        return texto.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
