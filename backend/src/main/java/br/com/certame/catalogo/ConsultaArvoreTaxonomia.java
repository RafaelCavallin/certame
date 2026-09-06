package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.AssuntoArvore;
import br.com.certame.catalogo.dto.DisciplinaArvore;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta a árvore disciplina/assunto com totais de questões, a partir da contagem agregada do banco. */
@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class ConsultaArvoreTaxonomia {
    private final DisciplinaRepository disciplinas;

    public ConsultaArvoreTaxonomia(DisciplinaRepository disciplinas) {
        this.disciplinas = disciplinas;
    }

    public List<DisciplinaArvore> arvore(boolean incluirArquivados) {
        var nos = new LinkedHashMap<UUID, NoArvore>();
        for (var linha : disciplinas.contarArvore(incluirArquivados)) {
            var no = nos.computeIfAbsent(linha.getDisciplinaId(), id -> new NoArvore(
                    id, linha.getDisciplinaNome(), linha.getDisciplinaArquivadaEm() != null));
            if (linha.getAssuntoId() == null) {
                no.totalQuestoes = linha.getTotalQuestoes();
                no.totalPublicadas = linha.getTotalPublicadas();
            } else {
                no.assuntos.add(new AssuntoArvore(linha.getAssuntoId(), linha.getAssuntoNome(),
                        linha.getTotalQuestoes(), linha.getTotalPublicadas(), linha.getAssuntoArquivadoEm() != null));
            }
        }
        return nos.values().stream()
                .map(no -> new DisciplinaArvore(no.id, no.nome, no.totalQuestoes, no.totalPublicadas, no.arquivado, no.assuntos))
                .toList();
    }

    public DisciplinaArvore encontrarDisciplina(UUID id, List<DisciplinaArvore> arvore) {
        return arvore.stream().filter(no -> no.id().equals(id)).findFirst().orElseThrow(RecursoNaoEncontradoException::new);
    }

    public AssuntoArvore encontrarAssunto(UUID id, List<DisciplinaArvore> arvore) {
        return arvore.stream().flatMap(no -> no.assuntos().stream()).filter(no -> no.id().equals(id))
                .findFirst().orElseThrow(RecursoNaoEncontradoException::new);
    }

    private static final class NoArvore {
        final UUID id;
        final String nome;
        final boolean arquivado;
        long totalQuestoes;
        long totalPublicadas;
        final List<AssuntoArvore> assuntos = new ArrayList<>();

        NoArvore(UUID id, String nome, boolean arquivado) {
            this.id = id;
            this.nome = nome;
            this.arquivado = arquivado;
        }
    }
}
