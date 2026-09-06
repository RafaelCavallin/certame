package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.DisciplinaArvore;
import br.com.certame.catalogo.dto.RequisicaoNoTaxonomia;
import br.com.certame.catalogo.edital.EditalConsultaVinculos;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.Metrics;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Criação, renomeação, exclusão e arquivamento de disciplinas na árvore de taxonomia. */
@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
public class DisciplinaTaxonomiaService {
    private final DisciplinaRepository disciplinas;
    private final RegistradorAuditoria auditoria;
    private final EditalConsultaVinculos editalVinculos;
    private final ConsultaArvoreTaxonomia consulta;

    public DisciplinaTaxonomiaService(DisciplinaRepository disciplinas, RegistradorAuditoria auditoria,
            EditalConsultaVinculos editalVinculos, ConsultaArvoreTaxonomia consulta) {
        this.disciplinas = disciplinas;
        this.auditoria = auditoria;
        this.editalVinculos = editalVinculos;
        this.consulta = consulta;
    }

    public DisciplinaArvore criar(RequisicaoNoTaxonomia requisicao) {
        var nome = requisicao.nome().trim();
        validarNome(nome);
        var entidade = disciplinas.save(new Disciplina(nome));
        auditoria.registrar("CRIAR", "disciplina", entidade.getId(), Map.of("nome", nome));
        return consulta.encontrarDisciplina(entidade.getId(), consulta.arvore(true));
    }

    public DisciplinaArvore renomear(UUID id, RequisicaoNoTaxonomia requisicao) {
        var entidade = disciplinas.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        var nome = requisicao.nome().trim();
        if (!entidade.getNome().equalsIgnoreCase(nome)) {
            validarNome(nome);
        }
        entidade.setNome(nome);
        disciplinas.save(entidade);
        auditoria.registrar("RENOMEAR", "disciplina", id, Map.of("nome", nome));
        return consulta.encontrarDisciplina(id, consulta.arvore(true));
    }

    public void excluir(UUID id) {
        var entidade = disciplinas.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        var total = disciplinas.countQuestoesAtivas(id);
        if (total > 0) {
            recusada("DISCIPLINA_EM_USO", "Esta disciplina tem " + total + " questões vinculadas e não pode ser excluída.");
        }
        if (editalVinculos.disciplinaEmUso(id)) {
            recusada("DISCIPLINA_EM_USO", "Esta disciplina está vinculada a um edital e não pode ser excluída.");
        }
        entidade.setExcluidoEm(Instant.now());
        disciplinas.save(entidade);
        auditoria.registrar("EXCLUIR", "disciplina", id, null);
    }

    public void alterarArquivamento(UUID id, boolean arquivar) {
        var entidade = disciplinas.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        entidade.setArquivadoEm(arquivar ? Instant.now() : null);
        disciplinas.save(entidade);
        auditoria.registrar(arquivar ? "ARQUIVAR" : "DESARQUIVAR", "disciplina", id, null);
    }

    private void validarNome(String nome) {
        if (disciplinas.existsByNomeIgnoreCaseAndExcluidoEmIsNull(nome)) {
            throw new ConflitoException("NOME_EM_USO", "Já existe uma disciplina com este nome.");
        }
    }

    private void recusada(String codigo, String mensagem) {
        Metrics.counter("certame.taxonomia.exclusao.recusada").increment();
        throw new ConflitoException(codigo, mensagem);
    }
}
