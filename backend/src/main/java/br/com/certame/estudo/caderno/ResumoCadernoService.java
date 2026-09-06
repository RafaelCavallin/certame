package br.com.certame.estudo.caderno;

import br.com.certame.estudo.caderno.dto.CadernoResumo;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta o CadernoResumo (total, respondidas e percentual de progresso) a partir do caderno. */
@Service
@Transactional(readOnly = true)
public class ResumoCadernoService {
    private final CadernoRepository cadernos;
    private final CadernoQuestaoRepository questoesCaderno;

    public ResumoCadernoService(CadernoRepository cadernos, CadernoQuestaoRepository questoesCaderno) {
        this.cadernos = cadernos;
        this.questoesCaderno = questoesCaderno;
    }

    public CadernoResumo resumo(Caderno caderno) {
        var total = (int) questoesCaderno.countByIdCadernoId(caderno.getId());
        var respondidas = respondidas(caderno.getUsuarioId(), caderno.getId());
        return resumo(caderno, total, respondidas);
    }

    public CadernoResumo resumo(Caderno caderno, int total, int respondidas) {
        return CadernoResumo.manual(caderno.getId(), caderno.getNome(), caderno.getDescricao(), total, respondidas,
                ProgressoCaderno.percentual(respondidas, total), caderno.getAtualizadoEm());
    }

    private int respondidas(UUID usuarioId, UUID cadernoId) {
        var total = cadernos.contarQuestoes(usuarioId).stream().filter(c -> c.getCadernoId().equals(cadernoId)).findFirst();
        return total.map(c -> (int) c.getQuestoesRespondidas()).orElse(0);
    }
}
