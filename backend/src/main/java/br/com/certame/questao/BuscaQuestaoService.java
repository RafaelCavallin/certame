package br.com.certame.questao;

import br.com.certame.comum.Paginacao;
import br.com.certame.comum.PaginaResposta;
import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.questao.dto.FiltroQuestao;
import br.com.certame.questao.dto.QuestaoResumo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BuscaQuestaoService {
    private static final Pattern CODIGO = Pattern.compile("^(?:[Qq]-?)?\\d+$");
    private final QuestaoRepository questoes;
    private final MeterRegistry metricas;

    public BuscaQuestaoService(QuestaoRepository questoes, MeterRegistry metricas) { this.questoes = questoes; this.metricas = metricas; }

    @PreAuthorize("hasAnyRole('PROFESSOR','ADMIN','SUPER_ADMIN')")
    public PaginaResposta<QuestaoResumo> buscar(FiltroQuestao filtro, Paginacao paginacao) {
        return executar(filtro, paginacao);
    }

    /** Listagem pública (`GET /api/v1/questoes`): o controlador já força {@code status=PUBLICADA} para quem não é professor+. */
    @PreAuthorize("isAuthenticated()")
    public PaginaResposta<QuestaoResumo> buscarPublica(FiltroQuestao filtro, Paginacao paginacao) {
        return executar(filtro, paginacao);
    }

    private PaginaResposta<QuestaoResumo> executar(FiltroQuestao filtro, Paginacao paginacao) {
        validar(filtro, paginacao);
        var inicio = Timer.start(metricas);
        try {
            var codigo = filtro.temTermo() && CODIGO.matcher(filtro.q().trim()).matches()
                    ? "Q-" + filtro.q().trim().replaceFirst("^[Qq]-?", "") : null;
            return questoes.buscar(filtro, paginacao, codigo);
        } finally {
            inicio.stop(Timer.builder("certame.questao.busca").tag("comFiltro", Boolean.toString(filtro.temFiltro()))
                    .tag("comTermo", Boolean.toString(filtro.temTermo())).publishPercentiles(0.95, 0.99).register(metricas));
        }
    }

    private void validar(FiltroQuestao filtro, Paginacao paginacao) {
        if (filtro.q() != null && filtro.q().length() > 200) throw new DadosInvalidosException(Map.of("q", "O termo deve ter no máximo 200 caracteres."));
        if (paginacao.page() < 0) throw new DadosInvalidosException(Map.of("page", "A página deve ser maior ou igual a zero."));
        if (paginacao.size() < 1 || paginacao.size() > 100) throw new DadosInvalidosException(Map.of("size", "O tamanho da página deve estar entre 1 e 100."));
        var partes = paginacao.sort().split(",", -1);
        if ((partes.length > 2) || !(partes[0].equals("atualizadoEm") || partes[0].equals("codigo") || partes[0].equals("criadoEm"))
                || (partes.length == 2 && !(partes[1].equals("asc") || partes[1].equals("desc"))))
            throw new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida."));
    }
}
