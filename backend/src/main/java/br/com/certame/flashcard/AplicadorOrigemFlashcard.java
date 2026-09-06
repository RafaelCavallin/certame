package br.com.certame.flashcard;

import br.com.certame.catalogo.Assunto;
import br.com.certame.catalogo.Disciplina;
import br.com.certame.flashcard.dto.RequisicaoFlashcard;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Preenche a origem (RF-28) de um flashcard a partir do contexto resolvido ou dos campos manuais do pedido. */
@Service
public class AplicadorOrigemFlashcard {
    private final EntityManager entityManager;

    public AplicadorOrigemFlashcard(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void aplicar(Flashcard card, ContextoOrigem contexto, RequisicaoFlashcard requisicao) {
        if (contexto.questao() == null) {
            aplicarManual(card, requisicao);
            return;
        }
        var origem = card.getOrigem();
        origem.setQuestaoOrigem(contexto.questao());
        origem.setSessaoOrigem(contexto.sessao());
        origem.setDisciplina(contexto.questao().getDisciplina());
        origem.setAssunto(contexto.questao().getAssunto());
    }

    public void aplicarManual(Flashcard card, RequisicaoFlashcard requisicao) {
        card.getOrigem().setDisciplina(referencia(Disciplina.class, requisicao.disciplinaId()));
        card.getOrigem().setAssunto(referencia(Assunto.class, requisicao.assuntoId()));
    }

    private <T> T referencia(Class<T> tipo, UUID id) {
        return id == null ? null : entityManager.getReference(tipo, id);
    }
}
