package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.Disciplina;
import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.comum.excecao.DadosInvalidosException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ResolvedorDisciplinasEdital {
    private static final String MENSAGEM_DISCIPLINA_INVALIDA =
            "Uma das disciplinas selecionadas não existe mais na árvore de matérias.";
    private final DisciplinaRepository disciplinas;

    public ResolvedorDisciplinasEdital(DisciplinaRepository disciplinas) { this.disciplinas = disciplinas; }

    public Set<Disciplina> resolver(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        var encontradas = disciplinas.findByIdInAndExcluidoEmIsNull(ids);
        if (encontradas.size() != ids.size()) {
            throw new DadosInvalidosException(Map.of("disciplinaIds", MENSAGEM_DISCIPLINA_INVALIDA));
        }
        return new HashSet<>(encontradas);
    }
}
