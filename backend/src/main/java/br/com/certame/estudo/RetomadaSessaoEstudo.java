package br.com.certame.estudo;

import br.com.certame.comum.excecao.RecursoNaoEncontradoException;
import br.com.certame.comum.excecao.SessaoEncerradaException;
import br.com.certame.estudo.dto.RespostaSessao;
import br.com.certame.estudo.dto.SessaoRetomavel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Retoma uma sessão de estudo em andamento (RF-33): a mais recente não finalizada nem expirada. */
@Component
public class RetomadaSessaoEstudo {
    private final SessaoEstudoRepository sessoes;
    private final MontadorRespostaSessao montadorResposta;
    private final TituloSessaoService titulos;

    public RetomadaSessaoEstudo(SessaoEstudoRepository sessoes, MontadorRespostaSessao montadorResposta,
            TituloSessaoService titulos) {
        this.sessoes = sessoes;
        this.montadorResposta = montadorResposta;
        this.titulos = titulos;
    }

    public RespostaSessao retomar(SessaoEstudo sessao) {
        if (sessao.getFinalizadaEm() != null) {
            throw new SessaoEncerradaException();
        }
        if (sessao.getExpiradaEm() != null) {
            throw new RecursoNaoEncontradoException();
        }
        return montadorResposta.respostaDaSessao(sessao);
    }

    public Optional<SessaoRetomavel> retomavel(UUID usuarioId) {
        return sessoes.findFirstByUsuarioIdAndModoNotAndFinalizadaEmIsNullAndExpiradaEmIsNullOrderByUltimaAtividadeEmDesc(
                        usuarioId, ModoSessao.PROVA)
                .map(s -> new SessaoRetomavel(s.getId(), s.getModo(), s.getOrigem(),
                        titulos.titulo(s.getId(), s.getOrigem(), s.getOrigemRef(), usuarioId),
                        montadorResposta.indiceAtual(s), s.getTamanho(), s.getUltimaAtividadeEm()));
    }
}
