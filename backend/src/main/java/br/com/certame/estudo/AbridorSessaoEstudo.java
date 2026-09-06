package br.com.certame.estudo;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SerieVaziaException;
import br.com.certame.estudo.caderno.ReferenciaCaderno;
import br.com.certame.estudo.caderno.SerieDoCadernoService;
import br.com.certame.estudo.dto.RequisicaoSessao;
import br.com.certame.estudo.dto.RespostaSessao;
import br.com.certame.questao.Questao;
import br.com.certame.questao.QuestaoRepository;
import br.com.certame.usuario.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/** Abre uma sessão de estudo, resolvendo a série de questões a partir da origem escolhida. */
@Component
public class AbridorSessaoEstudo {
    private final SessaoEstudoRepository sessoes;
    private final SessaoQuestaoRepository questoes;
    private final QuestaoRepository cadastroQuestoes;
    private final UsuarioRepository usuarios;
    private final MontadorSerie montador;
    private final SerieDoCadernoService serieDoCaderno;
    private final EstudoObservabilidade observabilidade;
    private final MontadorRespostaSessao montadorResposta;

    public AbridorSessaoEstudo(SessaoEstudoRepository sessoes, SessaoQuestaoRepository questoes,
            QuestaoRepository cadastroQuestoes, UsuarioRepository usuarios, MontadorSerie montador,
            SerieDoCadernoService serieDoCaderno, EstudoObservabilidade observabilidade,
            MontadorRespostaSessao montadorResposta) {
        this.sessoes = sessoes;
        this.questoes = questoes;
        this.cadastroQuestoes = cadastroQuestoes;
        this.usuarios = usuarios;
        this.montador = montador;
        this.serieDoCaderno = serieDoCaderno;
        this.observabilidade = observabilidade;
        this.montadorResposta = montadorResposta;
    }

    public RespostaSessao abrir(UUID usuarioId, RequisicaoSessao req) {
        if (req == null) {
            throw new DadosInvalidosException(Map.of("tamanho", "Escolha 10, 30 ou 50 questões."));
        }
        var origem = req.origemEfetiva();
        var ids = idsDaOrigem(origem, req, usuarioId);
        var modo = origem == OrigemSessao.CADERNO ? ModoSessao.FOCO : req.modoEfetivo();
        return abrir(usuarioId, modo, origem, req.origemRef(), ids);
    }

    public RespostaSessao abrir(UUID usuarioId, OrigemSessao origem, String origemRef, List<UUID> ids) {
        if (origem != OrigemSessao.SIMULADO || ids == null || ids.isEmpty()) {
            throw new DadosInvalidosException(Map.of("questoes", "A série do simulado não pode ser vazia."));
        }
        return abrir(usuarioId, ModoSessao.PROVA, origem, origemRef, ids);
    }

    private RespostaSessao abrir(UUID usuarioId, ModoSessao modo, OrigemSessao origem, String origemRef, List<UUID> ids) {
        var usuario = usuarios.findById(usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        var sessao = sessoes.save(new SessaoEstudo(usuario, modo, origem, origemRef, (short) ids.size()));
        var entidades = IntStream.range(0, ids.size())
                .mapToObj(i -> new SessaoQuestao(sessao, (short) (i + 1), questao(ids.get(i)))).toList();
        questoes.saveAll(entidades);
        return montadorResposta.montar(sessao, entidades, List.of());
    }

    private List<UUID> idsDaOrigem(OrigemSessao origem, RequisicaoSessao req, UUID usuarioId) {
        try {
            return origem == OrigemSessao.CADERNO ? idsDoCaderno(usuarioId, req.origemRef()) : idsLivres(req, usuarioId);
        } catch (SerieVaziaException excecao) {
            observabilidade.serieVazia(origem);
            throw excecao;
        }
    }

    private List<UUID> idsDoCaderno(UUID usuarioId, String origemRef) {
        var ids = serieDoCaderno.questoes(usuarioId, ReferenciaCaderno.de(origemRef));
        if (ids.isEmpty()) {
            throw new SerieVaziaException();
        }
        return ids;
    }

    private List<UUID> idsLivres(RequisicaoSessao req, UUID usuarioId) {
        if (req.tamanho() == null) {
            throw new DadosInvalidosException(Map.of("tamanho", "Escolha 10, 30 ou 50 questões."));
        }
        return montador.montar(req.filtroEfetivo(), new TamanhoSerie(req.tamanho()), usuarioId);
    }

    private Questao questao(UUID id) {
        return cadastroQuestoes.findByIdAndExcluidoEmIsNull(id).orElseThrow(RecursoNaoEncontradoException::new);
    }
}
