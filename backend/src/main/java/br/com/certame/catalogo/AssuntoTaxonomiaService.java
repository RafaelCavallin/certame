package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.AssuntoArvore;
import br.com.certame.catalogo.dto.RequisicaoNoTaxonomia;
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

/** Criação, renomeação, exclusão e arquivamento de assuntos na árvore de taxonomia. */
@Service
@Transactional
@PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
public class AssuntoTaxonomiaService {
    private final DisciplinaRepository disciplinas;
    private final AssuntoRepository assuntos;
    private final RegistradorAuditoria auditoria;
    private final ConsultaArvoreTaxonomia consulta;

    public AssuntoTaxonomiaService(DisciplinaRepository disciplinas, AssuntoRepository assuntos,
            RegistradorAuditoria auditoria, ConsultaArvoreTaxonomia consulta) {
        this.disciplinas = disciplinas;
        this.assuntos = assuntos;
        this.auditoria = auditoria;
        this.consulta = consulta;
    }

    public AssuntoArvore criar(RequisicaoNoTaxonomia requisicao) {
        if (requisicao.disciplinaId() == null) {
            throw new IllegalArgumentException("disciplinaId é obrigatório.");
        }
        var disciplina = disciplinas.findByIdAndExcluidoEmIsNull(requisicao.disciplinaId())
                .orElseThrow(RecursoNaoEncontradoException::new);
        var nome = requisicao.nome().trim();
        validarNome(disciplina.getId(), nome);
        var entidade = assuntos.save(new Assunto(disciplina, nome));
        auditoria.registrar("CRIAR", "assunto", entidade.getId(), Map.of("nome", nome, "disciplinaId", disciplina.getId()));
        return consulta.encontrarAssunto(entidade.getId(), consulta.arvore(true));
    }

    public AssuntoArvore renomear(UUID id, RequisicaoNoTaxonomia requisicao) {
        var entidade = assuntos.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        var nome = requisicao.nome().trim();
        if (!entidade.getNome().equalsIgnoreCase(nome)) {
            validarNome(entidade.getDisciplina().getId(), nome);
        }
        entidade.setNome(nome);
        assuntos.save(entidade);
        auditoria.registrar("RENOMEAR", "assunto", id, Map.of("nome", nome));
        return consulta.encontrarAssunto(id, consulta.arvore(true));
    }

    public void excluir(UUID id) {
        var entidade = assuntos.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        var total = assuntos.countQuestoesAtivas(id);
        if (total > 0) {
            recusada("ASSUNTO_EM_USO", "Este assunto tem " + total
                    + " questões vinculadas e não pode ser excluído. Arquive-o para retirá-lo da árvore.");
        }
        entidade.setExcluidoEm(Instant.now());
        assuntos.save(entidade);
        auditoria.registrar("EXCLUIR", "assunto", id, null);
    }

    public void alterarArquivamento(UUID id, boolean arquivar) {
        var entidade = assuntos.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
        entidade.setArquivadoEm(arquivar ? Instant.now() : null);
        assuntos.save(entidade);
        auditoria.registrar(arquivar ? "ARQUIVAR" : "DESARQUIVAR", "assunto", id, null);
    }

    private void validarNome(UUID disciplinaId, String nome) {
        if (assuntos.existsByDisciplinaIdAndNomeIgnoreCaseAndExcluidoEmIsNull(disciplinaId, nome)) {
            throw new ConflitoException("NOME_EM_USO", "Já existe um assunto com este nome nesta disciplina.");
        }
    }

    private void recusada(String codigo, String mensagem) {
        Metrics.counter("certame.taxonomia.exclusao.recusada").increment();
        throw new ConflitoException(codigo, mensagem);
    }
}
