package br.com.certame.questao;

import br.com.certame.catalogo.AssuntoRepository;
import br.com.certame.catalogo.BancaRepository;
import br.com.certame.catalogo.DisciplinaRepository;
import br.com.certame.catalogo.OrgaoRepository;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.questao.dto.AlternativaDto;
import br.com.certame.questao.dto.RequisicaoQuestao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Preenche uma Questao a partir do pedido de criação/edição, validando referências e alternativas. */
@Component
public class MontadorQuestao {
    private final DisciplinaRepository disciplinas;
    private final AssuntoRepository assuntos;
    private final BancaRepository bancas;
    private final OrgaoRepository orgaos;

    public MontadorQuestao(DisciplinaRepository disciplinas, AssuntoRepository assuntos,
            BancaRepository bancas, OrgaoRepository orgaos) {
        this.disciplinas = disciplinas;
        this.assuntos = assuntos;
        this.bancas = bancas;
        this.orgaos = orgaos;
    }

    public void preencher(Questao q, RequisicaoQuestao r) {
        q.setTipo(r.tipo());
        q.setEnunciado(r.enunciado().trim());
        q.setDisciplina(r.disciplinaId() == null ? null
                : disciplinas.findByIdAndExcluidoEmIsNull(r.disciplinaId()).orElseThrow(RecursoNaoEncontradoException::new));
        q.setAssunto(r.assuntoId() == null ? null
                : assuntos.findByIdAndExcluidoEmIsNull(r.assuntoId()).orElseThrow(RecursoNaoEncontradoException::new));
        if (q.getAssunto() != null
                && (q.getDisciplina() == null || !q.getAssunto().getDisciplina().getId().equals(q.getDisciplina().getId()))) {
            throw new DadosInvalidosException(Map.of("assuntoId", "O assunto escolhido não pertence à disciplina selecionada."));
        }
        q.setBanca(r.bancaId() == null ? null : bancas.findById(r.bancaId()).orElseThrow(RecursoNaoEncontradoException::new));
        q.setOrgao(r.orgaoId() == null ? null : orgaos.findById(r.orgaoId()).orElseThrow(RecursoNaoEncontradoException::new));
        q.setAno(r.ano());
        q.setDificuldade(r.dificuldade());
        q.setGabarito(r.gabarito());
        q.setComentario(r.comentario());
        q.getAlternativas().clear();
        if (r.tipo() == TipoQuestao.ME) {
            for (var a : ordenadas(r.alternativas())) {
                q.adicionarAlternativa(new Alternativa(letra(a.ordem()), a.texto().trim(), a.ordem()));
            }
        }
    }

    public void validarAlternativas(RequisicaoQuestao r) {
        if (r.tipo() == TipoQuestao.CE) {
            return;
        }
        var alternativas = r.alternativas() == null ? List.<AlternativaDto>of() : r.alternativas();
        if (alternativas.size() > 5) {
            throw new DadosInvalidosException(Map.of("alternativas", "Informe no máximo cinco alternativas."));
        }
        if (alternativas.stream().anyMatch(a -> a.ordem() == null)) {
            throw new DadosInvalidosException(Map.of("alternativas", "Informe a ordem de cada alternativa."));
        }
        var ordens = alternativas.stream().map(AlternativaDto::ordem).sorted().toList();
        for (int i = 0; i < ordens.size(); i++) {
            if (ordens.get(i) != i + 1) {
                throw new DadosInvalidosException(Map.of("alternativas", "As ordens devem ser contíguas e sem repetição."));
            }
        }
    }

    private List<AlternativaDto> ordenadas(List<AlternativaDto> alternativas) {
        return alternativas == null ? List.of() : alternativas.stream().sorted(Comparator.comparing(AlternativaDto::ordem)).toList();
    }

    private String letra(short ordem) {
        return String.valueOf((char) ('A' + ordem - 1));
    }
}
