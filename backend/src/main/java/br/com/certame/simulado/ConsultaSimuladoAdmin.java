package br.com.certame.simulado;

import br.com.certame.questao.QuestaoRepository;
import br.com.certame.questao.StatusQuestao;
import br.com.certame.simulado.dto.ResumoComposicao;
import br.com.certame.simulado.dto.SimuladoDetalhe;
import br.com.certame.simulado.dto.SimuladoResumoAdmin;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Monta os DTOs de leitura administrativa do simulado (detalhe, resumo e composição) a partir da entidade. */
@Component
public class ConsultaSimuladoAdmin {
    private final SimuladoComposicaoRepository composicoes;
    private final TentativaSimuladoRepository tentativas;
    private final QuestaoRepository questoes;
    private final JanelaSimulado janela;

    public ConsultaSimuladoAdmin(SimuladoComposicaoRepository composicoes, TentativaSimuladoRepository tentativas,
            QuestaoRepository questoes, JanelaSimulado janela) {
        this.composicoes = composicoes;
        this.tentativas = tentativas;
        this.questoes = questoes;
        this.janela = janela;
    }

    public SimuladoDetalhe detalhe(Simulado s) {
        return new SimuladoDetalhe(s.getId(), s.getCodigo(), s.getNome(), s.getDescricao(),
                s.getBanca() == null ? null : s.getBanca().getId(), s.getNumeroQuestoes(), s.getDuracaoSegundos(),
                s.getJanelaAbertura(), s.getJanelaEncerramento(), s.getStatus(), s.getPublicadoEm(),
                resumoComposicao(s, composicoes.findBySimuladoId(s.getId())));
    }

    public SimuladoResumoAdmin resumo(Simulado s) {
        var itens = composicoes.findBySimuladoId(s.getId());
        var soma = itens.stream().mapToInt(SimuladoComposicao::getQuantidade).sum();
        var concluidas = tentativas.countBySimuladoIdAndTipoAndStatus(s.getId(), TipoTentativa.VALIDA, StatusTentativa.FINALIZADA);
        return new SimuladoResumoAdmin(s.getId(), s.getCodigo(), s.getNome(),
                s.getBanca() == null ? null : s.getBanca().getNome(), s.getNumeroQuestoes(), s.getDuracaoSegundos(),
                janela.estado(s.getStatus(), s.getJanelaAbertura(), s.getJanelaEncerramento()),
                s.getJanelaAbertura(), s.getJanelaEncerramento(), concluidas, soma == s.getNumeroQuestoes());
    }

    public ResumoComposicao resumoComposicao(Simulado s, List<SimuladoComposicao> itens) {
        var resumo = itens.stream()
                .map(i -> new ResumoComposicao.ItemComposicaoResumo(i.getDisciplina().getId(), i.getDisciplina().getNome(),
                        i.getDificuldade(), i.getQuantidade(), disponiveis(i)))
                .toList();
        var soma = resumo.stream().mapToInt(ResumoComposicao.ItemComposicaoResumo::quantidade).sum();
        var suficiente = resumo.stream().allMatch(i -> i.disponiveis() >= i.quantidade());
        return new ResumoComposicao(resumo, soma, s.getNumeroQuestoes(), soma == s.getNumeroQuestoes(), suficiente);
    }

    private long disponiveis(SimuladoComposicao item) {
        return item.getDificuldade() == null
                ? questoes.countByDisciplinaIdAndStatusAndExcluidoEmIsNull(item.getDisciplina().getId(), StatusQuestao.PUBLICADA)
                : questoes.countByDisciplinaIdAndDificuldadeAndStatusAndExcluidoEmIsNull(item.getDisciplina().getId(),
                        item.getDificuldade(), StatusQuestao.PUBLICADA);
    }

    public Map<String, Object> janelaPayload(Simulado s) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("janelaAbertura", iso(s.getJanelaAbertura()));
        payload.put("janelaEncerramento", iso(s.getJanelaEncerramento()));
        return payload;
    }

    private String iso(Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
