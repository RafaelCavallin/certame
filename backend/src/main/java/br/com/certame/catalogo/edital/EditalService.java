package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.Banca;
import br.com.certame.catalogo.BancaRepository;
import br.com.certame.catalogo.Orgao;
import br.com.certame.catalogo.OrgaoRepository;
import br.com.certame.catalogo.edital.dto.*;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EditalService {
    private static final String MSG_NOME_EM_USO = "Já existe um edital com este nome.";
    private static final String MSG_BANCA_INVALIDA = "A banca selecionada não existe.";
    private static final String MSG_ORGAO_INVALIDA = "O órgão selecionado não existe.";

    private final EditalRepository editais;
    private final BancaRepository bancas;
    private final OrgaoRepository orgaos;
    private final ResolvedorDisciplinasEdital resolvedorDisciplinas;
    private final RegistradorAuditoria auditoria;

    public EditalService(EditalRepository editais, BancaRepository bancas, OrgaoRepository orgaos,
            ResolvedorDisciplinasEdital resolvedorDisciplinas, RegistradorAuditoria auditoria) {
        this.editais = editais; this.bancas = bancas; this.orgaos = orgaos;
        this.resolvedorDisciplinas = resolvedorDisciplinas; this.auditoria = auditoria;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public EditalDetalhe criar(RequisicaoEdital requisicao) {
        garantirNomeInedito(requisicao.nome(), null);
        var edital = new Edital(requisicao.nome().trim(), requisicao.status());
        aplicar(edital, requisicao);
        editais.save(edital);
        auditoria.registrar("CRIAR", "edital", edital.getId(), EditalAuditoria.payload(requisicao));
        return EditalDetalheMapeador.detalhar(edital);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public EditalDetalhe atualizar(UUID id, RequisicaoEdital requisicao) {
        var edital = buscarAtivo(id);
        garantirNomeInedito(requisicao.nome(), id);
        edital.setNome(requisicao.nome().trim());
        aplicar(edital, requisicao);
        editais.save(edital);
        auditoria.registrar("ATUALIZAR", "edital", id, EditalAuditoria.payload(requisicao));
        return EditalDetalheMapeador.detalhar(edital);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void excluir(UUID id) {
        var edital = buscarAtivo(id);
        edital.setExcluidoEm(Instant.now());
        editais.save(edital);
        auditoria.registrar("EXCLUIR", "edital", id, null);
    }

    private void aplicar(Edital edital, RequisicaoEdital requisicao) {
        EditalRegras.garantirPrazoQuandoAberto(requisicao.status(), requisicao.prazoInscricao());
        EditalRegras.garantirUrlValida(requisicao.urlOficial());
        edital.setStatus(requisicao.status());
        edital.setVagas(requisicao.vagas());
        edital.setBanca(resolverBanca(requisicao.bancaId()));
        edital.setOrgao(resolverOrgao(requisicao.orgaoId()));
        edital.setPrazoInscricao(requisicao.prazoInscricao());
        edital.setUrlOficial(requisicao.urlOficial());
        edital.setDisciplinas(resolvedorDisciplinas.resolver(requisicao.disciplinaIds()));
    }

    private void garantirNomeInedito(String nome, UUID idAtual) {
        var nomeNormalizado = nome.trim();
        var emUso = idAtual == null
                ? editais.existsByNomeIgnoreCaseAndExcluidoEmIsNull(nomeNormalizado)
                : editais.existsByNomeIgnoreCaseAndExcluidoEmIsNullAndIdNot(nomeNormalizado, idAtual);
        if (emUso) throw new ConflitoException("NOME_EM_USO", MSG_NOME_EM_USO);
    }

    private Banca resolverBanca(UUID bancaId) {
        if (bancaId == null) return null;
        return bancas.findById(bancaId).orElseThrow(() -> new DadosInvalidosException(Map.of("bancaId", MSG_BANCA_INVALIDA)));
    }

    private Orgao resolverOrgao(UUID orgaoId) {
        if (orgaoId == null) return null;
        return orgaos.findById(orgaoId).orElseThrow(() -> new DadosInvalidosException(Map.of("orgaoId", MSG_ORGAO_INVALIDA)));
    }

    private Edital buscarAtivo(UUID id) {
        return editais.findByIdAndExcluidoEmIsNull(id).orElseThrow(() -> new RecursoNaoEncontradoException("EDITAL_NAO_ENCONTRADO"));
    }
}
