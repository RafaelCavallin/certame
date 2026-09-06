package br.com.certame.simulado;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.simulado.dto.FiltroSimulado;
import br.com.certame.simulado.dto.ItemComposicao;
import br.com.certame.simulado.dto.RequisicaoSimulado;
import br.com.certame.simulado.dto.ResumoComposicao;
import br.com.certame.simulado.dto.SimuladoDetalhe;
import br.com.certame.simulado.dto.SimuladoResumoAdmin;
import br.com.certame.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SimuladoAdminService {
    private final SimuladoRepository simulados;
    private final GeradorCodigoSimulado gerador;
    private final RegistradorAuditoria auditoria;
    private final Clock clock;
    private final ConsultaSimuladoAdmin consulta;
    private final MontadorSimulado montador;
    private final ComposicaoSimuladoService composicaoService;
    private final LocalizadorSimulado localizador;

    public SimuladoAdminService(SimuladoRepository simulados, GeradorCodigoSimulado gerador,
            RegistradorAuditoria auditoria, Clock clock, ConsultaSimuladoAdmin consulta, MontadorSimulado montador,
            ComposicaoSimuladoService composicaoService, LocalizadorSimulado localizador) {
        this.simulados = simulados;
        this.gerador = gerador;
        this.auditoria = auditoria;
        this.clock = clock;
        this.consulta = consulta;
        this.montador = montador;
        this.composicaoService = composicaoService;
        this.localizador = localizador;
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public SimuladoDetalhe criar(RequisicaoSimulado requisicao) {
        return criar(requisicao, null);
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public SimuladoDetalhe criar(RequisicaoSimulado r, Usuario criador) {
        var simulado = new Simulado(gerador.gerar(), r.nome().trim(), r.numeroQuestoes(), r.duracaoSegundos(), criador);
        montador.preencher(simulado, r);
        var salvo = simulados.save(simulado);
        auditoria.registrar("SIMULADO_CRIADO", "simulado", salvo.getId(), consulta.janelaPayload(salvo));
        return consulta.detalhe(salvo);
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public SimuladoDetalhe atualizar(UUID id, RequisicaoSimulado r) {
        var simulado = localizador.obterAlteravel(id);
        var antes = consulta.janelaPayload(simulado);
        montador.preencher(simulado, r);
        var salvo = simulados.save(simulado);
        auditoria.registrar("SIMULADO_ATUALIZADO", "simulado", id, Map.of("antes", antes, "depois", consulta.janelaPayload(salvo)));
        return consulta.detalhe(salvo);
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public void excluir(UUID id) {
        var simulado = localizador.obterAlteravel(id);
        simulado.setExcluidoEm(Instant.now(clock));
        simulados.save(simulado);
        auditoria.registrar("SIMULADO_EXCLUIDO", "simulado", id, consulta.janelaPayload(simulado));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public Page<SimuladoResumoAdmin> listar(FiltroSimulado filtro, Paginacao paginacao) {
        var pageable = PageRequest.of(paginacao.page(), paginacao.size(), Sort.by("atualizadoEm").descending());
        var pagina = filtrosVazios(filtro) ? simulados.findByExcluidoEmIsNull(pageable)
                : simulados.buscarAdministrativos(normalizar(filtro.busca()), filtro.estado(), filtro.bancaId(), Instant.now(clock), pageable);
        return pagina.map(consulta::resumo);
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public SimuladoDetalhe detalhar(UUID id) {
        return consulta.detalhe(localizador.obter(id));
    }
    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public ResumoComposicao substituirComposicao(UUID id, List<ItemComposicao> itens) {
        return composicaoService.substituir(localizador.obterAlteravel(id), itens);
    }

    private boolean filtrosVazios(FiltroSimulado filtro) {
        return (filtro.busca() == null || filtro.busca().isBlank()) && filtro.estado() == null && filtro.bancaId() == null;
    }
    private String normalizar(String busca) {
        return busca == null || busca.isBlank() ? null : busca.trim();
    }
}
