package br.com.certame.flashcard.revisao;

import br.com.certame.comum.excecao.ConflitoException;
import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Localiza uma sessão de revisão do usuário e garante que ainda esteja aberta (RF-33/RF-34). */
@Component
public class LocalizadorSessaoRevisao {
    private final SessaoRevisaoRepository sessoes;

    public LocalizadorSessaoRevisao(SessaoRevisaoRepository sessoes) {
        this.sessoes = sessoes;
    }

    public SessaoRevisao aberta(UUID sessaoId, UUID usuarioId) {
        var sessao = sessoes.findByIdAndUsuarioId(sessaoId, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
        if (sessao.getFinalizadaEm() != null) {
            throw new ConflitoException("SESSAO_FINALIZADA", "Esta sessão de revisão já foi finalizada.");
        }
        return sessao;
    }
}
