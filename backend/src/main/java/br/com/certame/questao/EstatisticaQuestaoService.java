package br.com.certame.questao;

import br.com.certame.estudo.RespostaEstatisticaRepository;
import br.com.certame.questao.dto.EstatisticaQuestao;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EstatisticaQuestaoService {
    private final QuestaoRepository questoes;
    private final RespostaEstatisticaRepository respostas;
    private final QuestaoEstatisticaRepository estatisticas;
    private final Clock clock;

    public EstatisticaQuestaoService(QuestaoRepository questoes, RespostaEstatisticaRepository respostas,
                                    QuestaoEstatisticaRepository estatisticas, Clock clock) {
        this.questoes = questoes;
        this.respostas = respostas;
        this.estatisticas = estatisticas;
        this.clock = clock;
    }

    public EstatisticaQuestao recalcular(UUID questaoId) {
        var questao = questoes.findByIdAndExcluidoEmIsNull(questaoId)
                .orElseThrow(RecursoNaoEncontradoException::new);
        var agregado = respostas.contarValidas(questaoId, questao.getAutor().getId());
        Short percentual = agregado.totalRespostas() == 0
                ? null
                : (short) Math.round(100.0 * agregado.totalAcertos() / agregado.totalRespostas());
        var atualizadoEm = Instant.now(clock);
        var estatistica = estatisticas.findById(questaoId)
                .orElseGet(() -> new QuestaoEstatistica(questao));
        estatistica.atualizar(agregado.totalRespostas(), agregado.totalAcertos(), percentual, agregado.tempoMedioMs(), atualizadoEm);
        var salva = estatisticas.save(estatistica);
        return new EstatisticaQuestao(salva.getQuestaoId(), salva.getTotalRespostas(), salva.getTotalAcertos(),
                salva.getPctAcerto(), salva.getTempoMedioMs(), salva.getAtualizadoEm());
    }
}
