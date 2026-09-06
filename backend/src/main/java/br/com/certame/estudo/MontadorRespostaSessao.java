package br.com.certame.estudo;

import br.com.certame.estudo.dto.AlternativaDaSerie;
import br.com.certame.estudo.dto.MarcacoesQuestao;
import br.com.certame.estudo.dto.QuestaoDaSerie;
import br.com.certame.estudo.dto.RespostaRegistrada;
import br.com.certame.estudo.dto.RespostaSessao;
import br.com.certame.questao.StatusQuestao;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/** Monta o RespostaSessao (série visível ao aluno + respostas dadas) a partir das entidades de sessão. */
@Component
public class MontadorRespostaSessao {
    private final SessaoQuestaoRepository questoes;
    private final RespostaRepository respostas;
    private final MarcacaoService marcacoes;
    private final TempoSessao tempo;

    public MontadorRespostaSessao(SessaoQuestaoRepository questoes, RespostaRepository respostas,
            MarcacaoService marcacoes, TempoSessao tempo) {
        this.questoes = questoes;
        this.respostas = respostas;
        this.marcacoes = marcacoes;
        this.tempo = tempo;
    }

    public RespostaSessao respostaDaSessao(SessaoEstudo sessao) {
        return montar(sessao, questoes.findBySessaoIdOrderByIdOrdem(sessao.getId()),
                respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId()));
    }

    public RespostaSessao montar(SessaoEstudo sessao, List<SessaoQuestao> itens, List<Resposta> dadas) {
        var ativos = itens.stream()
                .filter(i -> i.getQuestao().getStatus() == StatusQuestao.PUBLICADA && i.getQuestao().getExcluidoEm() == null)
                .toList();
        var ids = ativos.stream().map(SessaoQuestao::getQuestaoId).toList();
        var marcadas = marcacoes.marcadasDa(sessao.getUsuario().getId(), ids);
        var serie = IntStream.range(0, ativos.size())
                .mapToObj(indice -> questaoDto(ativos.get(indice), marcadas.get(ids.get(indice)), (short) (indice + 1)))
                .toList();
        var dtoRespostas = dadas.stream().map(this::respostaDto).toList();
        return new RespostaSessao(sessao.getId(), sessao.getModo(), sessao.getOrigem(), sessao.getOrigemRef(),
                serie.size(), primeiraSemResposta(ids, dadas), sessao.getIniciadaEm(),
                tempo.duracaoSegundos(sessao.getIniciadaEm(), sessao.getFinalizadaEm(), sessao.getPausadaEm(), sessao.getPausadoMs()),
                serie, dtoRespostas);
    }

    public int indiceAtual(SessaoEstudo sessao) {
        return respostas.findBySessaoIdOrderByRespondidaEm(sessao.getId()).size();
    }

    private int primeiraSemResposta(List<UUID> ids, List<Resposta> dadas) {
        var respondidas = dadas.stream().map(r -> r.getQuestao().getId()).toList();
        return IntStream.range(0, ids.size()).filter(i -> !respondidas.contains(ids.get(i))).findFirst().orElse(ids.size());
    }

    private QuestaoDaSerie questaoDto(SessaoQuestao item, MarcacoesQuestao m, short ordem) {
        var q = item.getQuestao();
        var alternativas = q.getAlternativas().stream()
                .map(a -> new AlternativaDaSerie(a.getLetra(), a.getTexto(), a.getOrdem())).toList();
        return new QuestaoDaSerie(q.getId(), ordem, q.getCodigo(), q.getTipo(), q.getEnunciado(),
                q.getDisciplina() == null ? null : q.getDisciplina().getNome(),
                q.getAssunto() == null ? null : q.getAssunto().getNome(),
                q.getBanca() == null ? null : q.getBanca().getNome(),
                q.getOrgao() == null ? null : q.getOrgao().getNome(), q.getAno(), q.getDificuldade(), alternativas,
                m != null && m.favorita(), m != null && m.erroRecorrente());
    }

    private RespostaRegistrada respostaDto(Resposta r) {
        return new RespostaRegistrada(r.getQuestao().getId(), r.getMarcada(), r.isCorreta(), null, null, null,
                r.getTempoMs(), r.getRespondidaEm());
    }
}
