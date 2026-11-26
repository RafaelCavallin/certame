package br.com.certame.importacao.lote;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class GeradorCodigoLote {
    private final EntityManager entityManager;

    public GeradorCodigoLote(EntityManager entityManager) { this.entityManager = entityManager; }

    public String gerar() {
        Number proximo = (Number) entityManager.createNativeQuery("select nextval('lote_importacao_codigo_seq')").getSingleResult();
        return "IMP-%05d".formatted(proximo.longValue());
    }
}
