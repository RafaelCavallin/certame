package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.AssuntoArvore;
import br.com.certame.catalogo.dto.DisciplinaArvore;
import br.com.certame.catalogo.dto.RequisicaoNoTaxonomia;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Fachada da árvore de taxonomia: delega consulta, mutação de disciplinas e mutação de assuntos. */
@Service
@PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
public class TaxonomiaService {
    private final ConsultaArvoreTaxonomia consulta;
    private final DisciplinaTaxonomiaService disciplinaService;
    private final AssuntoTaxonomiaService assuntoService;

    public TaxonomiaService(ConsultaArvoreTaxonomia consulta, DisciplinaTaxonomiaService disciplinaService,
            AssuntoTaxonomiaService assuntoService) {
        this.consulta = consulta;
        this.disciplinaService = disciplinaService;
        this.assuntoService = assuntoService;
    }

    @PreAuthorize("isAuthenticated()")
    public List<DisciplinaArvore> arvore() {
        return consulta.arvore(false);
    }

    @PreAuthorize("isAuthenticated()")
    public List<DisciplinaArvore> arvore(boolean incluirArquivados) {
        return consulta.arvore(incluirArquivados);
    }

    public DisciplinaArvore criarDisciplina(RequisicaoNoTaxonomia requisicao) {
        return disciplinaService.criar(requisicao);
    }

    public DisciplinaArvore criarDisciplina(String nome) {
        return criarDisciplina(new RequisicaoNoTaxonomia(nome, null));
    }

    public DisciplinaArvore renomearDisciplina(UUID id, RequisicaoNoTaxonomia requisicao) {
        return disciplinaService.renomear(id, requisicao);
    }

    public DisciplinaArvore renomearDisciplina(UUID id, String nome) {
        return renomearDisciplina(id, new RequisicaoNoTaxonomia(nome, null));
    }

    public void excluirDisciplina(UUID id) {
        disciplinaService.excluir(id);
    }

    public void arquivarDisciplina(UUID id) {
        disciplinaService.alterarArquivamento(id, true);
    }

    public void desarquivarDisciplina(UUID id) {
        disciplinaService.alterarArquivamento(id, false);
    }

    public AssuntoArvore criarAssunto(RequisicaoNoTaxonomia requisicao) {
        return assuntoService.criar(requisicao);
    }

    public AssuntoArvore criarAssunto(UUID disciplinaId, String nome) {
        return criarAssunto(new RequisicaoNoTaxonomia(nome, disciplinaId));
    }

    public AssuntoArvore renomearAssunto(UUID id, RequisicaoNoTaxonomia requisicao) {
        return assuntoService.renomear(id, requisicao);
    }

    public AssuntoArvore renomearAssunto(UUID id, String nome) {
        return renomearAssunto(id, new RequisicaoNoTaxonomia(nome, null));
    }

    public void excluirAssunto(UUID id) {
        assuntoService.excluir(id);
    }

    public void arquivarAssunto(UUID id) {
        assuntoService.alterarArquivamento(id, true);
    }

    public void desarquivarAssunto(UUID id) {
        assuntoService.alterarArquivamento(id, false);
    }
}
