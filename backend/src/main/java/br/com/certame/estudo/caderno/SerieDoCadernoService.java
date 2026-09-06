package br.com.certame.estudo.caderno;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SerieDoCadernoService {
    private final CadernoService cadernos;
    private final CadernoAutomaticoService automaticos;
    private final CadernoQuestaoRepository questoesCaderno;
    private final CadernoObservabilidade observabilidade;

    public SerieDoCadernoService(CadernoService cadernos, CadernoAutomaticoService automaticos, CadernoQuestaoRepository questoesCaderno, CadernoObservabilidade observabilidade) {
        this.cadernos = cadernos; this.automaticos = automaticos; this.questoesCaderno = questoesCaderno; this.observabilidade = observabilidade;
    }

    @Transactional(readOnly = true)
    public List<UUID> questoes(UUID usuarioId, ReferenciaCaderno referencia) {
        var ids = referencia.automatico() ? automaticos.questoes(usuarioId, referencia.tipoAutomatico()) : cadernosQuestao(usuarioId, referencia.manualId());
        observabilidade.sessaoIniciada(referencia);
        return ids;
    }

    private List<UUID> cadernosQuestao(UUID usuarioId, UUID id) {
        cadernos.obter(usuarioId, id);
        return questoesCaderno.findByIdCadernoIdOrderByAdicionadaEmAsc(id).stream().map(CadernoQuestao::getQuestaoId).toList();
    }
}
