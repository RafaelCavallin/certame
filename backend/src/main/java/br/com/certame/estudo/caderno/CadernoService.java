package br.com.certame.estudo.caderno;

import br.com.certame.comum.excecao.CadernoAutomaticoImutavelException;
import br.com.certame.comum.excecao.NomeCadernoEmUsoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.estudo.caderno.dto.CadernoResumo;
import br.com.certame.estudo.caderno.dto.RequisicaoCaderno;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CadernoService {
    private final CadernoRepository cadernos;
    private final MutadorItensCaderno mutadorItens;
    private final ResumoCadernoService resumoService;
    private final Clock clock;

    public CadernoService(CadernoRepository cadernos, MutadorItensCaderno mutadorItens,
            ResumoCadernoService resumoService, Clock clock) {
        this.cadernos = cadernos;
        this.mutadorItens = mutadorItens;
        this.resumoService = resumoService;
        this.clock = clock;
    }

    @PreAuthorize("isAuthenticated()")
    public CadernoResumo criar(UUID usuarioId, RequisicaoCaderno requisicao) {
        if (cadernos.existsByUsuarioIdAndNomeIgnoreCaseAndExcluidoEmIsNull(usuarioId, requisicao.nome().trim())) {
            throw new NomeCadernoEmUsoException();
        }
        return salvarComNomeUnico(new Caderno(usuarioId, requisicao.nome().trim(), requisicao.descricao()));
    }

    @PreAuthorize("isAuthenticated()")
    public CadernoResumo atualizar(UUID usuarioId, UUID id, RequisicaoCaderno requisicao) {
        var caderno = obter(usuarioId, id);
        var nome = requisicao.nome().trim();
        if (cadernos.existsByUsuarioIdAndNomeIgnoreCaseAndExcluidoEmIsNull(usuarioId, nome) && !caderno.getNome().equalsIgnoreCase(nome)) {
            throw new NomeCadernoEmUsoException();
        }
        caderno.setNome(nome);
        caderno.setDescricao(requisicao.descricao());
        return salvarComNomeUnico(caderno);
    }
    public CadernoResumo atualizar(UUID usuarioId, String id, RequisicaoCaderno requisicao) { return atualizar(usuarioId, manual(id), requisicao); }

    @PreAuthorize("isAuthenticated()")
    public void excluir(UUID usuarioId, UUID id) {
        var caderno = obter(usuarioId, id);
        caderno.setExcluidoEm(Instant.now(clock));
        cadernos.save(caderno);
    }
    public void excluir(UUID usuarioId, String id) { excluir(usuarioId, manual(id)); }

    @PreAuthorize("isAuthenticated()")
    public CadernoResumo adicionarQuestoes(UUID usuarioId, UUID id, List<UUID> ids) {
        var caderno = obter(usuarioId, id);
        mutadorItens.adicionar(caderno, ids);
        cadernos.save(caderno);
        return resumo(caderno);
    }
    public CadernoResumo adicionarQuestoes(UUID usuarioId, String id, List<UUID> ids) { return adicionarQuestoes(usuarioId, manual(id), ids); }

    @PreAuthorize("isAuthenticated()")
    public void removerQuestao(UUID usuarioId, UUID id, UUID questaoId) {
        var caderno = obter(usuarioId, id);
        mutadorItens.remover(id, questaoId, caderno);
        cadernos.save(caderno);
    }
    public void removerQuestao(UUID usuarioId, String id, UUID questaoId) { removerQuestao(usuarioId, manual(id), questaoId); }

    @Transactional(readOnly = true)
    public Caderno obter(UUID usuarioId, UUID id) {
        return cadernos.findByIdAndUsuarioIdAndExcluidoEmIsNull(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CADERNO_NAO_ENCONTRADO"));
    }
    public CadernoResumo resumo(Caderno caderno) { return resumoService.resumo(caderno); }
    private CadernoResumo salvarComNomeUnico(Caderno caderno) {
        try {
            return resumo(cadernos.save(caderno));
        } catch (DataIntegrityViolationException ex) {
            throw new NomeCadernoEmUsoException();
        }
    }

    private UUID manual(String id) {
        var referencia = ReferenciaCaderno.de(id);
        if (referencia.automatico()) {
            throw new CadernoAutomaticoImutavelException();
        }
        return referencia.manualId();
    }
}
