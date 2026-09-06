package br.com.certame.flashcard;

import br.com.certame.comum.excecao.ConflitoException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transições de status de um flashcard: arquivar, restaurar e excluir logicamente. */
@Service
@PreAuthorize("isAuthenticated()")
public class FlashcardCicloVidaService {
    private final VerificadorDonoFlashcard verificador;
    private final Clock clock;

    public FlashcardCicloVidaService(VerificadorDonoFlashcard verificador, Clock clock) {
        this.verificador = verificador;
        this.clock = clock;
    }

    @Transactional
    public void arquivar(UUID id, UUID usuarioId) {
        var card = verificador.exigirProprio(id, usuarioId);
        if (card.getStatus() == StatusFlashcard.ARQUIVADO) throw conflito("CARD_JA_ARQUIVADO", "O flashcard já está arquivado.");
        card.setStatus(StatusFlashcard.ARQUIVADO);
        card.getRegistroTemporal().setArquivadoEm(Instant.now(clock));
    }

    @Transactional
    public void restaurar(UUID id, UUID usuarioId) {
        var card = verificador.exigirProprio(id, usuarioId);
        if (card.getStatus() == StatusFlashcard.ATIVO) throw conflito("CARD_JA_ATIVO", "O flashcard já está ativo.");
        card.setStatus(StatusFlashcard.ATIVO);
        card.getRegistroTemporal().setArquivadoEm(null);
    }

    @Transactional
    public void excluir(UUID id, UUID usuarioId) {
        var card = verificador.exigirProprio(id, usuarioId);
        card.setStatus(StatusFlashcard.EXCLUIDO);
        card.getRegistroTemporal().setExcluidoEm(Instant.now(clock));
    }

    private ConflitoException conflito(String code, String message) { return new ConflitoException(code, message); }
}
