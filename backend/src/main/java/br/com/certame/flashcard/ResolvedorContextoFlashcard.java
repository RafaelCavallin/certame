package br.com.certame.flashcard;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ResolvedorContextoFlashcard {
    private static final String ERRO_QUESTAO = "Questão de origem não encontrada.";
    private static final String ERRO_SESSAO = "Sessão de estudo não encontrada.";
    private final QuestaoPublicada questoes;
    private final SessaoDoUsuario sessoes;

    public ResolvedorContextoFlashcard(QuestaoPublicada questoes, SessaoDoUsuario sessoes) {
        this.questoes = questoes;
        this.sessoes = sessoes;
    }

    public ContextoOrigem resolver(UUID usuarioId, RequisicaoFlashcard requisicao) {
        validarSessaoSemQuestao(requisicao);
        if (requisicao.questaoOrigemId() == null) return ContextoOrigem.vazio();
        var questao = questoes.buscarPublicada(requisicao.questaoOrigemId()).orElseThrow(this::erroQuestao);
        var sessao = buscarSessao(usuarioId, requisicao.sessaoOrigemId());
        return new ContextoOrigem(questao, sessao);
    }

    private void validarSessaoSemQuestao(RequisicaoFlashcard requisicao) {
        if (requisicao.questaoOrigemId() == null && requisicao.sessaoOrigemId() != null) {
            throw erroSessao();
        }
    }

    private br.com.certame.estudo.SessaoEstudo buscarSessao(UUID usuarioId, UUID sessaoId) {
        if (sessaoId == null) return null;
        return sessoes.buscar(sessaoId, usuarioId).orElseThrow(this::erroSessao);
    }

    private DadosInvalidosException erroQuestao() {
        return new DadosInvalidosException(Map.of("questaoOrigemId", ERRO_QUESTAO));
    }

    private DadosInvalidosException erroSessao() {
        return new DadosInvalidosException(Map.of("sessaoOrigemId", ERRO_SESSAO));
    }
}
