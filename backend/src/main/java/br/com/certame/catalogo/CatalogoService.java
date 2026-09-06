package br.com.certame.catalogo;

import br.com.certame.catalogo.dto.*;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogoService {
    private final BancaRepository bancas; private final OrgaoRepository orgaos; private final RegistradorAuditoria auditoria;
    public CatalogoService(BancaRepository bancas, OrgaoRepository orgaos, RegistradorAuditoria auditoria) { this.bancas=bancas; this.orgaos=orgaos; this.auditoria=auditoria; }
    /** Leitura liberada a qualquer autenticado: o painel de filtros do estudo precisa da lista de bancas,
     * como já ocorre com a árvore de taxonomia. */
    @Transactional(readOnly = true) @PreAuthorize("isAuthenticated()")
    public List<ItemCatalogo> listarBancas() { return bancas.findAllByOrderByNomeAsc().stream().map(b -> new ItemCatalogo(b.getId(), b.getNome(), b.getSigla())).toList(); }
    @Transactional(readOnly = true) @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public List<ItemCatalogo> listarOrgaos() { return orgaos.findAllByOrderByNomeAsc().stream().map(o -> new ItemCatalogo(o.getId(), o.getNome(), o.getSigla())).toList(); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo criarBanca(RequisicaoItemCatalogo r) { validarBanca(r.nome()); var b=bancas.save(new Banca(r.nome().trim(), normalizarSigla(r.sigla()))); auditoria.registrar("CRIAR", "banca", b.getId(), r); return item(b); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo criarBanca(String nome, String sigla) { return criarBanca(new RequisicaoItemCatalogo(nome, sigla)); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo renomearBanca(UUID id, RequisicaoItemCatalogo r) { var b=bancas.findById(id).orElseThrow(RecursoNaoEncontradoException::new); if (!b.getNome().equalsIgnoreCase(r.nome().trim())) validarBanca(r.nome()); b.setNome(r.nome().trim()); b.setSigla(normalizarSigla(r.sigla())); bancas.save(b); auditoria.registrar("RENOMEAR", "banca", id, r); return item(b); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo renomearBanca(UUID id, String nome, String sigla) { return renomearBanca(id, new RequisicaoItemCatalogo(nome, sigla)); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public void excluirBanca(UUID id) { var b=bancas.findById(id).orElseThrow(RecursoNaoEncontradoException::new); bloquear(bancas.countQuestoesAtivas(id), "BANCA_EM_USO"); bancas.delete(b); auditoria.registrar("EXCLUIR", "banca", id, null); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo criarOrgao(RequisicaoItemCatalogo r) { validarOrgao(r.nome()); var o=orgaos.save(new Orgao(r.nome().trim(), normalizarSigla(r.sigla()))); auditoria.registrar("CRIAR", "orgao", o.getId(), r); return item(o); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo criarOrgao(String nome, String sigla) { return criarOrgao(new RequisicaoItemCatalogo(nome, sigla)); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo renomearOrgao(UUID id, RequisicaoItemCatalogo r) { var o=orgaos.findById(id).orElseThrow(RecursoNaoEncontradoException::new); if (!o.getNome().equalsIgnoreCase(r.nome().trim())) validarOrgao(r.nome()); o.setNome(r.nome().trim()); o.setSigla(normalizarSigla(r.sigla())); orgaos.save(o); auditoria.registrar("RENOMEAR", "orgao", id, r); return item(o); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public ItemCatalogo renomearOrgao(UUID id, String nome, String sigla) { return renomearOrgao(id, new RequisicaoItemCatalogo(nome, sigla)); }
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public void excluirOrgao(UUID id) { var o=orgaos.findById(id).orElseThrow(RecursoNaoEncontradoException::new); bloquear(orgaos.countQuestoesAtivas(id), "ORGAO_EM_USO"); orgaos.delete(o); auditoria.registrar("EXCLUIR", "orgao", id, null); }
    private void validarBanca(String nome) { if (bancas.existsByNomeIgnoreCase(nome.trim())) throw new ConflitoException("NOME_EM_USO", "Já existe uma banca com este nome."); }
    private void validarOrgao(String nome) { if (orgaos.existsByNomeIgnoreCase(nome.trim())) throw new ConflitoException("NOME_EM_USO", "Já existe um órgão com este nome."); }
    private void bloquear(long total, String codigo) { if (total > 0) throw new ConflitoException(codigo, "Este item tem " + total + " questões vinculadas e não pode ser excluído."); }
    private String normalizarSigla(String sigla) { return sigla == null || sigla.isBlank() ? null : sigla.trim(); }
    private ItemCatalogo item(Banca b) { return new ItemCatalogo(b.getId(), b.getNome(), b.getSigla()); }
    private ItemCatalogo item(Orgao o) { return new ItemCatalogo(o.getId(), o.getNome(), o.getSigla()); }
}
