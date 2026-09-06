package br.com.certame.flashcard;

import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalizadorTags {
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAMANHO_TAG = 30;

    public List<String> normalizar(List<String> brutas) {
        if (brutas == null) return List.of();
        var tags = new LinkedHashSet<String>();
        for (var bruta : brutas) adicionar(tags, bruta);
        if (tags.size() > MAX_TAGS) throw erroTags();
        return new ArrayList<>(tags);
    }

    private void adicionar(LinkedHashSet<String> tags, String bruta) {
        if (bruta == null) return;
        var tag = bruta.trim().toLowerCase(Locale.ROOT);
        if (tag.isEmpty()) return;
        if (tag.length() > MAX_TAMANHO_TAG) throw erroTags();
        tags.add(tag);
    }

    private DadosInvalidosException erroTags() {
        return new DadosInvalidosException(Map.of("tags", "Informe no máximo 10 tags de até 30 caracteres."));
    }
}
