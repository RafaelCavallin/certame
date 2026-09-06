package br.com.certame.questao;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.questao.dto.FiltroQuestao;
import br.com.certame.questao.dto.QuestaoResumo;

public interface QuestaoBuscaRepository {
    PaginaResposta<QuestaoResumo> buscar(FiltroQuestao filtro, Paginacao paginacao, String termoCodigo);
}
