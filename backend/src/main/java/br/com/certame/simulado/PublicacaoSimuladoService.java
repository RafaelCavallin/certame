package br.com.certame.simulado;

import br.com.certame.comum.auditoria.RegistradorAuditoria;
import br.com.certame.comum.excecao.*;
import br.com.certame.questao.QuestaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublicacaoSimuladoService {
    private final SimuladoRepository simulados;
    private final SimuladoComposicaoRepository composicoes;
    private final SimuladoQuestaoRepository questoesDoSimulado;
    private final TentativaSimuladoRepository tentativas;
    private final QuestaoRepository questoes;
    private final ValidadorPublicacaoSimulado validador;
    private final MontadorProvaSimulado montador;
    private final RegistradorAuditoria auditoria;
    private final Clock clock;

    public PublicacaoSimuladoService(SimuladoRepository simulados, SimuladoComposicaoRepository composicoes,
            SimuladoQuestaoRepository questoesDoSimulado, TentativaSimuladoRepository tentativas, QuestaoRepository questoes,
            ValidadorPublicacaoSimulado validador, MontadorProvaSimulado montador,
            RegistradorAuditoria auditoria, Clock clock) {
        this.simulados = simulados; this.composicoes = composicoes; this.questoesDoSimulado = questoesDoSimulado; this.tentativas = tentativas;
        this.questoes = questoes; this.validador = validador; this.montador = montador; this.auditoria = auditoria; this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Simulado publicar(UUID id) {
        var simulado = obter(id); garantirRascunho(simulado);
        var composicao = composicoes.findBySimuladoId(id);
        var pendencias = validador.validar(simulado, composicao, acervo(composicao));
        if (!pendencias.isEmpty()) throw new PublicacaoInvalidaException(pendencias);
        var serie = montador.montar(id, composicao);
        gravarSerie(simulado, serie); simulado.setPublicadoEm(Instant.now(clock)); simulado.setStatus(StatusSimulado.PUBLICADO);
        var salvo = simulados.save(simulado);
        auditoria.registrar("SIMULADO_PUBLICADO", "simulado", id, janelaPayload(salvo));
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Simulado despublicar(UUID id) {
        var simulado = obter(id); garantirSemTentativaFinalizada(id);
        questoesDoSimulado.deleteBySimuladoId(id); simulado.setPublicadoEm(null); simulado.setStatus(StatusSimulado.RASCUNHO);
        var salvo = simulados.save(simulado);
        auditoria.registrar("SIMULADO_DESPUBLICADO", "simulado", id, janelaPayload(salvo));
        return salvo;
    }

    private void garantirRascunho(Simulado simulado) {
        if (simulado.getStatus() != StatusSimulado.RASCUNHO) throw new ConflitoException("SIMULADO_EM_USO", "O simulado já foi publicado e não pode ser republicado.");
    }

    private Map<UUID, Long> acervo(List<SimuladoComposicao> composicao) {
        var acervo = new HashMap<UUID, Long>();
        for (var item : composicao) acervo.put(item.getDisciplina().getId(), questoes.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(item.getDisciplina().getId(), br.com.certame.questao.StatusQuestao.PUBLICADA));
        return acervo;
    }

    private void gravarSerie(Simulado simulado, List<UUID> serie) {
        questoesDoSimulado.deleteBySimuladoId(simulado.getId());
        var congeladas = new ArrayList<SimuladoQuestao>();
        for (var indice = 0; indice < serie.size(); indice++) {
            var questao = questoes.findById(serie.get(indice)).orElseThrow(RecursoNaoEncontradoException::new);
            congeladas.add(new SimuladoQuestao(simulado, (short) (indice + 1), questao));
        }
        questoesDoSimulado.saveAll(congeladas);
    }

    private Simulado obter(UUID id) { return simulados.findByIdAndExcluidoEmIsNull(id).orElseThrow(() -> new RecursoNaoEncontradoException("SIMULADO_NAO_ENCONTRADO")); }
    private void garantirSemTentativaFinalizada(UUID id) { if (tentativas.existsBySimuladoIdAndStatus(id, StatusTentativa.FINALIZADA)) throw new ConflitoException("SIMULADO_EM_USO", "O simulado possui tentativa finalizada e não pode ser alterado."); }
    private Map<String, Object> janelaPayload(Simulado s) { var payload = new LinkedHashMap<String, Object>(); payload.put("janelaAbertura", iso(s.getJanelaAbertura())); payload.put("janelaEncerramento", iso(s.getJanelaEncerramento())); return payload; }
    private String iso(Instant instante) { return instante == null ? null : instante.toString(); }
}
