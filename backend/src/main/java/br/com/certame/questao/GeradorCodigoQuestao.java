package br.com.certame.questao;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class GeradorCodigoQuestao {
    private final EntityManager entityManager;

    public GeradorCodigoQuestao(EntityManager entityManager) { this.entityManager = entityManager; }

    public String gerar() {
        Number proximo = (Number) entityManager.createNativeQuery("select nextval('questao_codigo_seq')").getSingleResult();
        return "Q-%05d".formatted(proximo.longValue());
    }
}
