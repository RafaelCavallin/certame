package br.com.certame.simulado;

import br.com.certame.simulado.dto.*;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimuladoCatalogoService {
    private final SimuladoRepository simulados;
    private final TentativaSimuladoRepository tentativas;
    private final JanelaSimulado janela;

    public SimuladoCatalogoService(SimuladoRepository simulados, TentativaSimuladoRepository tentativas, JanelaSimulado janela) {
        this.simulados = simulados; this.tentativas = tentativas; this.janela = janela;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public RespostaSimulados listarDoEstudante(UUID usuarioId) {
        var abertas = tentativas.findByUsuarioIdAndStatus(usuarioId, StatusTentativa.EM_ANDAMENTO);
        var concluidas = tentativas.findByUsuarioIdAndStatus(usuarioId, StatusTentativa.FINALIZADA).stream()
                .filter(t -> t.getTipo() == TipoTentativa.VALIDA).toList();
        var disponiveis = simulados.findByStatusAndExcluidoEmIsNull(StatusSimulado.PUBLICADO).stream()
                .map(simulado -> item(simulado, abertas)).filter(item -> item.estado() == EstadoSimulado.DISPONIVEL)
                .sorted(Comparator.comparing(SimuladoResumo::janelaEncerramento)).toList();
        var agendados = simulados.findByStatusAndExcluidoEmIsNull(StatusSimulado.PUBLICADO).stream()
                .map(simulado -> item(simulado, abertas)).filter(item -> item.estado() == EstadoSimulado.AGENDADO)
                .sorted(Comparator.comparing(SimuladoResumo::janelaAbertura)).toList();
        var historico = concluidas.stream().map(this::concluido).sorted(Comparator.comparing(SimuladoConcluido::concluidoEm).reversed()).toList();
        return new RespostaSimulados(agendados.stream().findFirst().orElse(null), disponiveis, agendados, historico);
    }

    private SimuladoResumo item(Simulado s, java.util.List<TentativaSimulado> abertas) {
        var tentativa = abertas.stream().filter(t -> t.getSimulado().getId().equals(s.getId())).findFirst().orElse(null);
        return new SimuladoResumo(s.getId(), s.getCodigo(), s.getNome(), s.getBanca() == null ? null : s.getBanca().getNome(), s.getNumeroQuestoes(), s.getDuracaoSegundos(), janela.estado(s.getStatus(), s.getJanelaAbertura(), s.getJanelaEncerramento()), s.getJanelaAbertura(), s.getJanelaEncerramento(), tentativa == null ? null : tentativa.getId());
    }

    private SimuladoConcluido concluido(TentativaSimulado tentativa) {
        var s = tentativa.getSimulado();
        var estado = janela.estado(s.getStatus(), s.getJanelaAbertura(), s.getJanelaEncerramento());
        return new SimuladoConcluido(s.getId(), s.getCodigo(), s.getNome(), s.getBanca() == null ? null : s.getBanca().getNome(), s.getNumeroQuestoes(), s.getDuracaoSegundos(), tentativa.getId(), tentativa.getNota(), tentativa.getAcertos(), tentativa.getErros(), tentativa.getNaoRespondidas(), tentativa.getTempoTotalSegundos(), tentativa.getFinalizadaEm(), estado == EstadoSimulado.DISPONIVEL);
    }
}
