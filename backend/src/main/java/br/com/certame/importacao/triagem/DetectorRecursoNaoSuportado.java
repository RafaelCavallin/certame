package br.com.certame.importacao.triagem;

import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** RF-45: recursos que o Certame não sabe exibir em enunciado, alternativa, comentário ou texto-base. */
@Component
public class DetectorRecursoNaoSuportado {
    public enum RecursoNaoSuportado { HTML, LATEX, IMAGEM_OU_ANEXO, TABELA, CARACTERE_CONTROLE }

    private static final Pattern HTML = Pattern.compile("</?[a-zA-Z][^<>]*>");
    private static final Pattern LATEX = Pattern.compile("\\$[^$\\n]+\\$|\\\\\\(.+?\\\\\\)|\\\\\\[.+?\\\\\\]");
    private static final Pattern IMAGEM_OU_ANEXO = Pattern.compile(
            "(?i)!\\[[^]]*]\\([^)]*\\)|\\.(png|jpe?g|gif|svg|pdf|docx?)\\b|\\banexo\\b");
    private static final Pattern TABELA = Pattern.compile("\\|[^|\\n]*\\|[^|\\n]*\\|");
    private static final Pattern CARACTERE_CONTROLE = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    public Optional<RecursoNaoSuportado> primeiraOcorrencia(String... campos) {
        for (var campo : campos) {
            var encontrado = detectarEm(campo);
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }
        return Optional.empty();
    }

    private Optional<RecursoNaoSuportado> detectarEm(String campo) {
        if (campo == null || campo.isBlank()) {
            return Optional.empty();
        }
        if (HTML.matcher(campo).find()) {
            return Optional.of(RecursoNaoSuportado.HTML);
        }
        if (LATEX.matcher(campo).find()) {
            return Optional.of(RecursoNaoSuportado.LATEX);
        }
        if (IMAGEM_OU_ANEXO.matcher(campo).find()) {
            return Optional.of(RecursoNaoSuportado.IMAGEM_OU_ANEXO);
        }
        if (TABELA.matcher(campo).find()) {
            return Optional.of(RecursoNaoSuportado.TABELA);
        }
        if (CARACTERE_CONTROLE.matcher(campo).find()) {
            return Optional.of(RecursoNaoSuportado.CARACTERE_CONTROLE);
        }
        return Optional.empty();
    }
}
