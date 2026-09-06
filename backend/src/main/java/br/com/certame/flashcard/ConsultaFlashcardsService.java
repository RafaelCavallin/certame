package br.com.certame.flashcard;

import br.com.certame.comum.excecao.DadosInvalidosException;
import br.com.certame.flashcard.dto.FlashcardResumo;
import br.com.certame.flashcard.dto.IndicadoresFlashcards;
import br.com.certame.flashcard.dto.PaginaFlashcards;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("isAuthenticated()")
@Transactional(readOnly = true)
public class ConsultaFlashcardsService {
    private static final Set<String> COLUNAS_ORDENACAO = Set.of("criadoEm", "proximaRevisao");
    private static final Set<EstadoFlashcard> ESTADOS_EM_FIXACAO =
            Set.of(EstadoFlashcard.NOVO, EstadoFlashcard.APRENDENDO, EstadoFlashcard.REAPRENDENDO);
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final int DIAS_PROXIMOS = 7;
    private final FlashcardRepository flashcards;
    private final VerificadorDonoFlashcard verificador;
    private final Clock clock;

    public ConsultaFlashcardsService(FlashcardRepository flashcards, VerificadorDonoFlashcard verificador, Clock clock) {
        this.flashcards = flashcards;
        this.verificador = verificador;
        this.clock = clock;
    }

    public PaginaFlashcards listar(UUID usuarioId, FiltroFlashcards filtro, Pageable pageable) {
        validarPaginacao(pageable);
        var status = filtro.arquivados() ? StatusFlashcard.ARQUIVADO : StatusFlashcard.ATIVO;
        var pagina = flashcards.buscar(usuarioId, filtro, status, pageable);
        var conteudo = pagina.getContent().stream().map(FlashcardResumo::de).toList();
        return new PaginaFlashcards(conteudo, pageable.getPageNumber(), pageable.getPageSize(),
                pagina.getTotalElements(), indicadores(usuarioId));
    }

    public FlashcardResumo buscar(UUID id, UUID usuarioId) {
        return FlashcardResumo.de(verificador.exigirProprio(id, usuarioId));
    }

    private IndicadoresFlashcards indicadores(UUID usuarioId) {
        var hoje = LocalDate.now(clock);
        var vencendoHoje = flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(usuarioId, StatusFlashcard.ATIVO, hoje);
        var proximosSeteDias = flashcards.countByUsuarioIdAndStatusAndAgendamentoProximaRevisaoLessThanEqual(
                usuarioId, StatusFlashcard.ATIVO, hoje.plusDays(DIAS_PROXIMOS));
        var emFixacao = flashcards.countByUsuarioIdAndStatusAndAgendamentoEstadoIn(usuarioId, StatusFlashcard.ATIVO, ESTADOS_EM_FIXACAO);
        var total = flashcards.countByUsuarioIdAndStatus(usuarioId, StatusFlashcard.ATIVO);
        var decks = flashcards.contarDecksComCardAtivo(usuarioId, StatusFlashcard.ATIVO);
        return new IndicadoresFlashcards(vencendoHoje, proximosSeteDias, emFixacao, total, decks);
    }

    private void validarPaginacao(Pageable pageable) {
        if (pageable.getPageSize() > TAMANHO_MAXIMO_PAGINA) {
            throw new DadosInvalidosException(Map.of("size", "O tamanho da página deve ser no máximo 100."));
        }
        validarOrdenacao(pageable.getSort());
    }

    private void validarOrdenacao(Sort sort) {
        for (var ordem : sort) {
            if (!COLUNAS_ORDENACAO.contains(ordem.getProperty())) {
                throw new DadosInvalidosException(Map.of("sort", "A ordenação informada não é válida."));
            }
        }
    }
}
