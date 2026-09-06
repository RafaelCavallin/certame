package br.com.certame.questao.dto;

import br.com.certame.questao.StatusQuestao;
import br.com.certame.questao.TipoQuestao;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FiltroQuestao(@Size(max = 200) String q, UUID disciplina, UUID assunto, UUID banca,
                            Integer ano, TipoQuestao tipo, StatusQuestao status) {
    public boolean temTermo() { return q != null && !q.isBlank(); }
    public boolean temFiltro() {
        return disciplina != null || assunto != null || banca != null || ano != null || tipo != null || status != null;
    }
}
